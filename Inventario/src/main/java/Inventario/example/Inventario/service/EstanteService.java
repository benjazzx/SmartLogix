package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.EstanteRequestDTO;
import Inventario.example.Inventario.dto.EstanteResponseDTO;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.repository.EstanteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstanteService {

    private final EstanteRepository estanteRepository;

    @Transactional(readOnly = true)
    public List<EstanteResponseDTO> listarTodos() {
        return estanteRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstanteResponseDTO> listarActivos() {
        return estanteRepository.findByActivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EstanteResponseDTO obtenerPorId(Long id) {
        EstanteModel estante = estanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstanteModel no encontrado con id: " + id));
        return toResponseDTO(estante);
    }

    @Transactional(readOnly = true)
    public List<EstanteResponseDTO> listarPorPasillo(Long idPasillo) {
        log.info("Listando estantes del pasillo: {}", idPasillo);
        return estanteRepository.findByPasilloId(idPasillo).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstanteResponseDTO> listarPorBodega(Long idBodega) {
        log.info("Listando estantes de la bodega: {}", idBodega);
        return estanteRepository.findByBodegaId(idBodega).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EstanteResponseDTO crear(EstanteRequestDTO dto) {
        log.info("Creando estante con código: {}", dto.getCodigo());
        if (estanteRepository.existsByCodigoIgnoreCase(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un estante con código: " + dto.getCodigo());
        }
        EstanteModel estante = EstanteModel.builder()
                .codigo(dto.getCodigo())
                .descripcion(dto.getDescripcion())
                .numNiveles(dto.getNumNiveles())
                .capacidadPorNivel(dto.getCapacidadPorNivel())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();
        EstanteModel guardado = estanteRepository.save(estante);
        log.info("EstanteModel creado con id: {}", guardado.getIdEstante());
        return toResponseDTO(guardado);
    }

    @Transactional
    public EstanteResponseDTO actualizar(Long id, EstanteRequestDTO dto) {
        log.info("Actualizando estante con id: {}", id);
        EstanteModel estante = estanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstanteModel no encontrado con id: " + id));

        if (!estante.getCodigo().equalsIgnoreCase(dto.getCodigo())
                && estanteRepository.existsByCodigoIgnoreCase(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un estante con código: " + dto.getCodigo());
        }

        estante.setCodigo(dto.getCodigo());
        estante.setDescripcion(dto.getDescripcion());
        estante.setNumNiveles(dto.getNumNiveles());
        estante.setCapacidadPorNivel(dto.getCapacidadPorNivel());
        if (dto.getActivo() != null) estante.setActivo(dto.getActivo());

        return toResponseDTO(estanteRepository.save(estante));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando (lógico) estante con id: {}", id);
        EstanteModel estante = estanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstanteModel no encontrado con id: " + id));
        estante.setActivo(false);
        estanteRepository.save(estante);
    }

    private EstanteResponseDTO toResponseDTO(EstanteModel e) {
        Double capTotal = (e.getNumNiveles() != null && e.getCapacidadPorNivel() != null)
                ? e.getNumNiveles() * e.getCapacidadPorNivel()
                : null;
        return EstanteResponseDTO.builder()
                .idEstante(e.getIdEstante())
                .codigo(e.getCodigo())
                .descripcion(e.getDescripcion())
                .numNiveles(e.getNumNiveles())
                .capacidadPorNivel(e.getCapacidadPorNivel())
                .capacidadTotal(capTotal)
                .activo(e.getActivo())
                .totalPasillosAsignados(e.getPasillos() != null ? e.getPasillos().size() : 0)
                .fechaCreacion(e.getFechaCreacion())
                .fechaActualizacion(e.getFechaActualizacion())
                .build();
    }
}
