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
    private static final UUID ESTADO_DEVOLUCION_SOLICITADA = UUID.fromString("b74c4d3e-2957-4ade-92a6-16d83f5c4d97");
    private static final UUID ESTADO_DEVOLUCION_APROBADA = UUID.fromString("fb9a31dc-29b5-44a0-80ed-a89871061da6");
    private static final UUID ESTADO_DEVOLUCION_RECHAZADA = UUID.fromString("4e3a4d28-3108-4906-8bbe-b0cbb8b0479c");

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
        OrdenModel saved = ordenRepository.save(orden);

        List<DetalleOrdenModel> detalles = new ArrayList<>();
        List<OrdenRequestDto.DetalleDto> reservados = new ArrayList<>();
        for (OrdenRequestDto.DetalleDto d : dto.getDetalles()) {
            var productoData = productoClient.getProducto(d.getProductoId());
            if (productoData == null) {
                revertirReservas(reservados, saved.getId());
                throw new IllegalStateException(
                    "Producto no disponible: " + d.getProductoId() + ". Intente nuevamente más tarde.");
            }
            // Reserva atómica (check + descuento en un solo UPDATE en Producto) — evita que dos
            // pedidos concurrentes lean el mismo stock "disponible" y ambos pasen la validación.
            boolean reservado = productoClient.reservarStock(d.getProductoId(), d.getCantidad(), saved.getId());
            if (!reservado) {
                revertirReservas(reservados, saved.getId());
                throw new IllegalStateException(
                    "Stock insuficiente para " + ProductoClient.extraerNombre(productoData)
                        + ": no hay " + d.getCantidad() + " unidad(es) disponibles.");
            }
            reservados.add(d);
            detalles.add(OrdenFactory.crearDetalle(
                saved, d.getProductoId(),
                ProductoClient.extraerNombre(productoData),
                ProductoClient.extraerPrecio(productoData),
                d.getCantidad()
            ));
        }

        saved.setDetalles(detalles);
        saved = ordenRepository.save(saved);

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

    // Si un producto de la orden falla la reserva, devuelve el stock de los que sí se habían
    // reservado antes de fallar — para no dejar unidades bloqueadas por un pedido que no se creó.
    private void revertirReservas(List<OrdenRequestDto.DetalleDto> reservados, Long ordenId) {
        for (OrdenRequestDto.DetalleDto d : reservados) {
            productoClient.registrarDevolucion(d.getProductoId(), d.getCantidad(), false, ordenId);
        }
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
        return ordenRepository.findByUserIdWithDetails(userId).stream()
                .map(OrdenResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto> getAll(String rolNombre, UUID requestingUserId) {
        return ordenRepository.findAllWithDetails().stream()
                .map(o -> OrdenResponseDto.from(o, rolNombre, requestingUserId))
                .toList();
    }

    @Transactional
    public OrdenResponseDto addHistorial(Long ordenId, HistorialRequestDto dto,
                                         UUID requestingUserId, String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(ordenId);

        validarTransicionSecuencial(orden.getEstadoActual(), dto.getEstadoNombre());

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
        h.setEstadoId(ESTADO_DEVOLUCION_SOLICITADA);
        h.setEstadoNombre("Devolución solicitada");
        h.setComentario("Cliente solicitó devolución: " + motivo);
        h.setFecha(java.time.LocalDateTime.now());
        historialRepository.save(h);

        return OrdenResponseDto.from(orden, ROL_CLIENTE, userId);
    }

    @Transactional
    public OrdenResponseDto resolverDevolucion(Long ordenId, boolean aprobada, boolean danado, String comentario) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if (!"Devolución solicitada".equals(orden.getEstadoActual())) {
            throw new IllegalStateException("La orden no tiene una devolución pendiente de resolver");
        }
        Hibernate.initialize(orden.getDetalles());
        Hibernate.initialize(orden.getHistorial());

        String nuevoEstado = aprobada ? "Devolución aprobada" : "Devolución rechazada";
        UUID estadoId = aprobada ? ESTADO_DEVOLUCION_APROBADA : ESTADO_DEVOLUCION_RECHAZADA;
        orden.setEstadoActual(nuevoEstado);
        ordenRepository.save(orden);

        if (aprobada) {
            for (DetalleOrdenModel detalle : orden.getDetalles()) {
                productoClient.registrarDevolucion(detalle.getProductoId(), detalle.getCantidad(), danado, ordenId);
            }
        }

        HistorialModel h = new HistorialModel();
        h.setOrden(orden);
        h.setEstadoId(estadoId);
        h.setEstadoNombre(nuevoEstado);
        h.setComentario(comentario != null && !comentario.isBlank() ? comentario
                : aprobada ? (danado ? "Devolución aprobada — producto dañado, registrado como merma"
                                     : "Devolución aprobada — stock reintegrado")
                           : "Devolución rechazada");
        h.setFecha(java.time.LocalDateTime.now());
        historialRepository.save(h);

        return OrdenResponseDto.from(orden);
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

    // No se puede saltar pasos (ej. despachar sin aceptar antes). Cancelado es la
    // excepción, válido desde cualquier estado previo a la entrega.
    private static final List<String> SECUENCIA_ESTADOS = List.of(
        "pendiente", "procesando", "aprobado", "en tránsito", "entregado"
    );

    private void validarTransicionSecuencial(String estadoActual, String estadoNuevo) {
        String actual = estadoActual == null ? "" : estadoActual.toLowerCase().trim();
        String nuevo  = estadoNuevo  == null ? "" : estadoNuevo.toLowerCase().trim();
        int idxActual = SECUENCIA_ESTADOS.indexOf(actual);
        int idxNuevo  = SECUENCIA_ESTADOS.indexOf(nuevo);

        if ("cancelado".equals(nuevo)) {
            int idxEntregado = SECUENCIA_ESTADOS.indexOf("entregado");
            if (idxActual < 0 || idxActual >= idxEntregado) {
                throw new IllegalStateException("No se puede cancelar una orden que ya fue entregada.");
            }
            return;
        }

        // Estado fuera de la secuencia principal (ej. devolución), no se valida acá.
        if (idxNuevo < 0) {
            return;
        }
        if (idxNuevo != idxActual + 1) {
            String siguienteValido = SECUENCIA_ESTADOS.get(
                Math.min(Math.max(idxActual, 0) + 1, SECUENCIA_ESTADOS.size() - 1));
            throw new IllegalStateException(
                "No se puede pasar de \"" + estadoActual + "\" a \"" + estadoNuevo
                + "\" directamente. El siguiente estado válido es \"" + siguienteValido + "\".");
        }
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
