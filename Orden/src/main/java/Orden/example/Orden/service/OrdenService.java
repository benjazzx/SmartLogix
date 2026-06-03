package Orden.example.Orden.service;

import Orden.example.Orden.client.EstadoClient;
import Orden.example.Orden.client.ProductoClient;
import Orden.example.Orden.client.UsersClient;
import Orden.example.Orden.dto.*;
import Orden.example.Orden.factory.OrdenFactory;
import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import Orden.example.Orden.messaging.OrdenEventProducer;
import Orden.example.Orden.repository.HistorialRepository;
import Orden.example.Orden.repository.OrdenRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrdenService {

    private static final String ROL_CLIENTE       = "cliente";
    private static final String ROL_TRANSPORTISTA = "transportista";

    @Autowired private OrdenRepository ordenRepository;
    @Autowired private HistorialRepository historialRepository;
    @Autowired private UsersClient usersClient;
    @Autowired private EstadoClient estadoClient;
    @Autowired private ProductoClient productoClient;
    @Autowired private OrdenEventProducer eventProducer;

    @Transactional
    public OrdenResponseDto createOrden(OrdenRequestDto dto, UUID userId) {
        String nombre = usersClient.getNombreUsuario(userId);
        if (nombre == null) {
            nombre = dto.getUserNombre();
        }
        String direccionTexto = usersClient.getDireccionTexto(userId);

        OrdenModel orden = OrdenFactory.crearOrden(userId, nombre, dto.getDireccionId());
        orden.setDireccionTexto(direccionTexto);

        List<DetalleOrdenModel> detalles = new ArrayList<>();
        for (OrdenRequestDto.DetalleDto d : dto.getDetalles()) {
            var productoData = productoClient.getProducto(d.getProductoId());
            if (productoData == null) {
                throw new IllegalStateException(
                    "Producto no disponible: " + d.getProductoId() + ". Intente nuevamente más tarde.");
            }
            detalles.add(OrdenFactory.crearDetalle(
                orden, d.getProductoId(),
                ProductoClient.extraerNombre(productoData),
                ProductoClient.extraerPrecio(productoData),
                d.getCantidad()
            ));
        }

        orden.setDetalles(detalles);
        OrdenModel saved = ordenRepository.save(orden);

        OrdenCreadaEvent event = new OrdenCreadaEvent(
            saved.getId(), userId, nombre, dto.getDireccionId(), saved.getFechaOrden(),
            saved.getDetalles().stream()
                .map(d -> new OrdenCreadaEvent.DetalleDto(
                        d.getProductoId(), d.getCantidad(),
                        d.getProductoNombre(), d.getPrecioUnitario()))
                .toList()
        );
        eventProducer.publishOrdenCreada(event);

        return OrdenResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public OrdenResponseDto getById(Long id, UUID requestingUserId, String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(id);
        if (ROL_CLIENTE.equals(rolNombre) && !orden.getUserId().equals(requestingUserId)) {
            throw new IllegalStateException("Acceso denegado: la orden no pertenece al usuario");
        }
        return OrdenResponseDto.from(orden, rolNombre, requestingUserId);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto> getMisOrdenes(UUID userId) {
        return ordenRepository.findByUserId(userId).stream()
                .map(OrdenResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto> getAll(String rolNombre, UUID requestingUserId) {
        return ordenRepository.findAll().stream()
                .map(o -> OrdenResponseDto.from(o, rolNombre, requestingUserId))
                .toList();
    }

    @Transactional
    public OrdenResponseDto addHistorial(Long ordenId, HistorialRequestDto dto,
                                         UUID requestingUserId, String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(ordenId);

        if (ROL_CLIENTE.equals(rolNombre)) {
            validarPermisosCliente(orden, dto, requestingUserId);
        }

        estadoClient.existeEstado(dto.getEstadoId());

        // Si el DTO trae quién lo hizo, usarlo; si no, resolver desde Users
        UUID realizadoPorId = dto.getRealizadoPorId() != null ? dto.getRealizadoPorId() : requestingUserId;
        String realizadoPorNombre = dto.getRealizadoPorNombre() != null
            ? dto.getRealizadoPorNombre()
            : usersClient.getNombreUsuario(requestingUserId);

        HistorialModel historial = OrdenFactory.crearHistorial(
            orden, dto.getEstadoId(), dto.getEstadoNombre(), dto.getComentario(),
            realizadoPorId, realizadoPorNombre
        );

        historialRepository.save(historial);

        orden.setEstadoActual(dto.getEstadoNombre());
        orden.getHistorial().add(historial);
        ordenRepository.save(orden);

        EstadoOrdenEvent event = new EstadoOrdenEvent(
            ordenId, orden.getUserId(), dto.getEstadoId(),
            dto.getEstadoNombre(), dto.getComentario(), historial.getFecha()
        );
        eventProducer.publishEstadoOrden(event);

        return OrdenResponseDto.from(orden, rolNombre, requestingUserId);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto.HistorialDto> getHistorial(Long ordenId,
                                                             UUID requestingUserId,
                                                             String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if (ROL_CLIENTE.equals(rolNombre) && !orden.getUserId().equals(requestingUserId)) {
            throw new IllegalStateException("Acceso denegado: la orden no pertenece al usuario");
        }
        return historialRepository.findByOrden(orden).stream()
                .map(OrdenResponseDto.HistorialDto::from)
                .toList();
    }

    @Transactional
    public OrdenResponseDto tomarOrden(Long ordenId, UUID transportistaId) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if (orden.isTomada()) {
            throw new IllegalStateException("La orden ya fue tomada por otro transportista");
        }
        Hibernate.initialize(orden.getDetalles());
        Hibernate.initialize(orden.getHistorial());

        String nombre = usersClient.getNombreUsuario(transportistaId);
        orden.setTomada(true);
        orden.setTransportistaId(transportistaId);
        orden.setTransportistaNombre(nombre);
        ordenRepository.save(orden);
        return OrdenResponseDto.from(orden, ROL_TRANSPORTISTA, transportistaId);
    }

    @Transactional
    public OrdenResponseDto liberarOrden(Long ordenId, UUID transportistaId) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if (!orden.isTomada() || !transportistaId.equals(orden.getTransportistaId())) {
            throw new IllegalStateException("No puedes liberar esta orden");
        }
        Hibernate.initialize(orden.getDetalles());
        Hibernate.initialize(orden.getHistorial());

        orden.setTomada(false);
        orden.setTransportistaId(null);
        orden.setTransportistaNombre(null);
        ordenRepository.save(orden);
        return OrdenResponseDto.from(orden, ROL_TRANSPORTISTA, transportistaId);
    }

    @Transactional
    public OrdenResponseDto solicitarDevolucion(Long ordenId, UUID userId, String motivo) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if (!orden.getUserId().equals(userId)) {
            throw new IllegalStateException("Acceso denegado");
        }
        if (!"Entregado".equals(orden.getEstadoActual())) {
            throw new IllegalStateException("Solo se puede solicitar devolución de órdenes entregadas");
        }
        Hibernate.initialize(orden.getDetalles());
        Hibernate.initialize(orden.getHistorial());

        orden.setEstadoActual("Devolución solicitada");
        orden.setMotivoDevolucion(motivo);
        ordenRepository.save(orden);

        HistorialModel h = new HistorialModel();
        h.setOrden(orden);
        h.setEstadoNombre("Devolución solicitada");
        h.setComentario("Cliente solicitó devolución: " + motivo);
        h.setFecha(java.time.LocalDateTime.now());
        historialRepository.save(h);

        return OrdenResponseDto.from(orden, ROL_CLIENTE, userId);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getResumenEmpleados() {
        List<HistorialModel> todos = historialRepository.findAll();
        java.util.Map<String, java.util.Map<String, Object>> resumen = new java.util.LinkedHashMap<>();

        for (HistorialModel h : todos) {
            if (h.getRealizadoPorNombre() == null || h.getRealizadoPorNombre().isBlank()) continue;
            String nombre = h.getRealizadoPorNombre();
            resumen.computeIfAbsent(nombre, k -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("empleado", nombre);
                m.put("totalAcciones", 0);
                m.put("ordenesProcesadas", new java.util.HashSet<Long>());
                return m;
            });
            java.util.Map<String, Object> emp = resumen.get(nombre);
            emp.put("totalAcciones", (int) emp.get("totalAcciones") + 1);
            @SuppressWarnings("unchecked")
            java.util.Set<Long> ids = (java.util.Set<Long>) emp.get("ordenesProcesadas");
            if (h.getOrden() != null) ids.add(h.getOrden().getId());
        }

        // Convertir set a count
        return resumen.values().stream().map(m -> {
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>(m);
            @SuppressWarnings("unchecked")
            java.util.Set<Long> ids = (java.util.Set<Long>) m.get("ordenesProcesadas");
            out.put("ordenesProcesadas", ids.size());
            return out;
        }).toList();
    }

    private void validarPermisosCliente(OrdenModel orden, HistorialRequestDto dto, UUID userId) {
        if (!orden.getUserId().equals(userId)) {
            throw new IllegalStateException("Acceso denegado: la orden no pertenece al usuario");
        }
        String estadoActual  = orden.getEstadoActual() != null ? orden.getEstadoActual().toLowerCase().trim() : "";
        boolean esCancelacion  = "Cancelado".equalsIgnoreCase(dto.getEstadoNombre());
        boolean esConfirmacion = "Entregado".equalsIgnoreCase(dto.getEstadoNombre());

        if (!esCancelacion && !esConfirmacion) {
            throw new IllegalStateException("Acceso denegado: el cliente solo puede cancelar o confirmar entrega");
        }
        if (esCancelacion && !estadoActual.equals("pendiente") && !estadoActual.equals("procesando")) {
            throw new IllegalStateException("Solo se pueden cancelar órdenes en estado Pendiente o Procesando");
        }
        if (esConfirmacion && !estadoActual.equals("entregado")) {
            throw new IllegalStateException("Solo se puede confirmar recibo cuando el transportista marcó la orden como Entregada");
        }
    }

    private OrdenModel findOrdenOrThrow(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Orden no encontrada: " + id));
    }
}
