package Orden.example.Orden.service;

import Orden.example.Orden.client.EstadoClient;
import Orden.example.Orden.client.UsersClient;
import Orden.example.Orden.dto.*;
import Orden.example.Orden.model.DetalleOrdenModel;
import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import Orden.example.Orden.messaging.OrdenEventProducer;
import Orden.example.Orden.repository.HistorialRepository;
import Orden.example.Orden.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrdenService {

    @Autowired private OrdenRepository ordenRepository;
    @Autowired private HistorialRepository historialRepository;
    @Autowired private UsersClient usersClient;
    @Autowired private EstadoClient estadoClient;
    @Autowired private OrdenEventProducer eventProducer;

    @Transactional
    public OrdenResponseDto createOrden(OrdenRequestDto dto, UUID userId) {
        // Intenta enriquecer userNombre desde Users; usa el del request si falla
        String nombre = usersClient.getNombreUsuario(userId);
        if (nombre == null) {
            nombre = dto.getUserNombre();
        }

        OrdenModel orden = new OrdenModel();
        orden.setUserId(userId);
        orden.setUserNombre(nombre);
        orden.setDireccionId(dto.getDireccionId());
        orden.setFechaOrden(LocalDateTime.now());
        orden.setEstadoActual("pendiente");

        List<DetalleOrdenModel> detalles = dto.getDetalles().stream().map(d -> {
            DetalleOrdenModel det = new DetalleOrdenModel();
            det.setOrden(orden);
            det.setProductoId(d.getProductoId());
            det.setCantidad(d.getCantidad());
            return det;
        }).collect(Collectors.toList());

        orden.setDetalles(detalles);
        OrdenModel saved = ordenRepository.save(orden);

        OrdenCreadaEvent event = new OrdenCreadaEvent(
            saved.getId(), userId, nombre, dto.getDireccionId(), saved.getFechaOrden(),
            dto.getDetalles().stream()
                .map(d -> new OrdenCreadaEvent.DetalleDto(d.getProductoId(), d.getCantidad()))
                .collect(Collectors.toList())
        );
        eventProducer.publishOrdenCreada(event);

        return OrdenResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public OrdenResponseDto getById(Long id, UUID requestingUserId, String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(id);
        if ("cliente".equals(rolNombre) && !orden.getUserId().equals(requestingUserId)) {
            throw new RuntimeException("Acceso denegado: la orden no pertenece al usuario");
        }
        return OrdenResponseDto.from(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto> getMisOrdenes(UUID userId) {
        return ordenRepository.findByUserId(userId).stream()
                .map(OrdenResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto> getAll() {
        return ordenRepository.findAll().stream()
                .map(OrdenResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdenResponseDto addHistorial(Long ordenId, HistorialRequestDto dto,
                                         UUID requestingUserId, String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(ordenId);

        if ("cliente".equals(rolNombre) && !orden.getUserId().equals(requestingUserId)) {
            throw new RuntimeException("Acceso denegado: la orden no pertenece al usuario");
        }

        estadoClient.existeEstado(dto.getEstadoId());

        HistorialModel historial = new HistorialModel();
        historial.setOrden(orden);
        historial.setEstadoId(dto.getEstadoId());
        historial.setEstadoNombre(dto.getEstadoNombre());
        historial.setComentario(dto.getComentario());
        historial.setFecha(LocalDateTime.now());

        historialRepository.save(historial);

        orden.setEstadoActual(dto.getEstadoNombre());
        orden.getHistorial().add(historial);
        ordenRepository.save(orden);

        EstadoOrdenEvent event = new EstadoOrdenEvent(
            ordenId, orden.getUserId(), dto.getEstadoId(),
            dto.getEstadoNombre(), dto.getComentario(), historial.getFecha()
        );
        eventProducer.publishEstadoOrden(event);

        return OrdenResponseDto.from(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponseDto.HistorialDto> getHistorial(Long ordenId,
                                                             UUID requestingUserId,
                                                             String rolNombre) {
        OrdenModel orden = findOrdenOrThrow(ordenId);
        if ("cliente".equals(rolNombre) && !orden.getUserId().equals(requestingUserId)) {
            throw new RuntimeException("Acceso denegado: la orden no pertenece al usuario");
        }
        return historialRepository.findByOrden(orden).stream()
                .map(OrdenResponseDto.HistorialDto::from)
                .collect(Collectors.toList());
    }

    private OrdenModel findOrdenOrThrow(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + id));
    }
}
