package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.BodegaActualizadaEvent;
import Inventario.example.Inventario.dto.BodegaRequestDTO;
import Inventario.example.Inventario.dto.BodegaResponseDTO;
import Inventario.example.Inventario.messaging.InventarioEventProducer;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BodegaService {

    private final BodegaRepository bodegaRepository;
    private final InventarioEventProducer eventProducer;

    @Transactional(readOnly = true)
    public List<BodegaResponseDTO> listarTodas() {
        log.info("Listando todas las bodegas");
        return bodegaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BodegaResponseDTO> listarActivas() {
        log.info("Listando bodegas activas");
        return bodegaRepository.findByActivaTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BodegaResponseDTO obtenerPorId(Long id) {
        log.info("Obteniendo bodega con id: {}", id);
        BodegaModel bodega = bodegaRepository.findByIdWithPasillos(id)
                .orElseThrow(() -> new EntityNotFoundException("BodegaModel no encontrada con id: " + id));
        return toResponseDTO(bodega);
    }

    @Transactional
    public BodegaResponseDTO crear(BodegaRequestDTO dto) {
        log.info("Creando nueva bodega: {}", dto.getNombre());
        if (bodegaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una bodega con el nombre: " + dto.getNombre());
        }
        BodegaModel bodega = BodegaModel.builder()
                .nombre(dto.getNombre())
                .direccion(dto.getDireccion())
                .ciudad(dto.getCiudad())
                .pais(dto.getPais())
                .capacidadTotal(dto.getCapacidadTotal())
                .activa(dto.getActiva() != null ? dto.getActiva() : true)
                .build();
        BodegaModel guardada = bodegaRepository.save(bodega);
        log.info("BodegaModel creada con id: {}", guardada.getIdBodega());
        eventProducer.publishBodegaActualizada(toBodegaEvent(guardada, "CREADA"));
        return toResponseDTO(guardada);
    }

    @Transactional
    public BodegaResponseDTO actualizar(Long id, BodegaRequestDTO dto) {
        log.info("Actualizando bodega con id: {}", id);
        BodegaModel bodega = bodegaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BodegaModel no encontrada con id: " + id));

        if (!bodega.getNombre().equalsIgnoreCase(dto.getNombre())
                && bodegaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe una bodega con el nombre: " + dto.getNombre());
        }

        bodega.setNombre(dto.getNombre());
        bodega.setDireccion(dto.getDireccion());
        bodega.setCiudad(dto.getCiudad());
        bodega.setPais(dto.getPais());
        bodega.setCapacidadTotal(dto.getCapacidadTotal());
        if (dto.getActiva() != null) bodega.setActiva(dto.getActiva());

        BodegaModel actualizada = bodegaRepository.save(bodega);
        eventProducer.publishBodegaActualizada(toBodegaEvent(actualizada, "ACTUALIZADA"));
        return toResponseDTO(actualizada);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando bodega con id: {}", id);
        BodegaModel bodega = bodegaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BodegaModel no encontrada con id: " + id));
        bodega.setActiva(false);
        bodegaRepository.save(bodega);
        log.info("BodegaModel desactivada (eliminación lógica) id: {}", id);
        eventProducer.publishBodegaActualizada(toBodegaEvent(bodega, "DESACTIVADA"));
    }

    private BodegaResponseDTO toResponseDTO(BodegaModel b) {
        return BodegaResponseDTO.builder()
                .idBodega(b.getIdBodega())
                .nombre(b.getNombre())
                .direccion(b.getDireccion())
                .ciudad(b.getCiudad())
                .pais(b.getPais())
                .capacidadTotal(b.getCapacidadTotal())
                .activa(b.getActiva())
                .totalPasillos(b.getPasillos() != null ? b.getPasillos().size() : 0)
                .fechaCreacion(b.getFechaCreacion())
                .fechaActualizacion(b.getFechaActualizacion())
                .build();
    }

    private BodegaActualizadaEvent toBodegaEvent(BodegaModel b, String tipo) {
        return new BodegaActualizadaEvent(
                b.getIdBodega(), b.getNombre(), b.getCiudad(),
                b.getPais(), b.getActiva(), tipo, LocalDateTime.now());
    }
}
