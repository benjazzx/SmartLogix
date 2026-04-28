package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.EstPasiRequestDTO;
import Inventario.example.Inventario.dto.EstPasiResponseDTO;
import Inventario.example.Inventario.dto.UbicacionActualizadaEvent;
import Inventario.example.Inventario.messaging.InventarioEventProducer;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
import Inventario.example.Inventario.repository.PasilloRepository;
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
public class EstPasiService {

    private final EstPasiRepository estPasiRepository;
    private final EstanteRepository estanteRepository;
    private final PasilloRepository pasilloRepository;
    private final InventarioEventProducer eventProducer;

    @Transactional(readOnly = true)
    public List<EstPasiResponseDTO> listarTodos() {
        return estPasiRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstPasiResponseDTO> listarPorPasillo(Long idPasillo) {
        log.info("Listando est_pasi del pasillo: {}", idPasillo);
        return estPasiRepository.findByPasilloOrdenado(idPasillo).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstPasiResponseDTO> listarPorEstante(Long idEstante) {
        log.info("Listando est_pasi del estante: {}", idEstante);
        return estPasiRepository.findByEstante_IdEstante(idEstante).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstPasiResponseDTO> listarPorBodega(Long idBodega) {
        log.info("Listando est_pasi de la bodega: {}", idBodega);
        return estPasiRepository.findByBodegaId(idBodega).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EstPasiResponseDTO obtenerPorId(Long id) {
        EstPasiModel ep = estPasiRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstPasiModel no encontrado con id: " + id));
        return toResponseDTO(ep);
    }

    @Transactional(readOnly = true)
    public Double calcularOcupacionPromedioBodega(Long idBodega) {
        Double pct = estPasiRepository.calcularOcupacionPromedioPorBodega(idBodega);
        return pct != null ? pct : 0.0;
    }

    @Transactional
    public EstPasiResponseDTO crear(EstPasiRequestDTO dto) {
        log.info("Asignando estante {} al pasillo {}", dto.getIdEstante(), dto.getIdPasillo());

        if (estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(dto.getIdEstante(), dto.getIdPasillo())) {
            throw new IllegalArgumentException(
                    "El estante " + dto.getIdEstante() + " ya está asignado al pasillo " + dto.getIdPasillo());
        }

        EstanteModel estante = estanteRepository.findById(dto.getIdEstante())
                .orElseThrow(() -> new EntityNotFoundException("EstanteModel no encontrado con id: " + dto.getIdEstante()));

        PasilloModel pasillo = pasilloRepository.findById(dto.getIdPasillo())
                .orElseThrow(() -> new EntityNotFoundException("PasilloModel no encontrado con id: " + dto.getIdPasillo()));

        EstPasiModel ep = EstPasiModel.builder()
                .estante(estante)
                .pasillo(pasillo)
                .posicion(dto.getPosicion())
                .numeroFila(dto.getNumeroFila())
                .ocupacionPct(dto.getOcupacionPct() != null ? dto.getOcupacionPct() : 0.0)
                .habilitada(dto.getHabilitada() != null ? dto.getHabilitada() : true)
                .observaciones(dto.getObservaciones())
                .build();

        EstPasiModel guardado = estPasiRepository.save(ep);
        log.info("EstPasiModel creado con id: {}", guardado.getIdEstPasi());
        eventProducer.publishUbicacionActualizada(toUbicacionEvent(guardado, "CREADA"));
        return toResponseDTO(guardado);
    }

    @Transactional
    public EstPasiResponseDTO actualizar(Long id, EstPasiRequestDTO dto) {
        log.info("Actualizando EstPasiModel con id: {}", id);
        EstPasiModel ep = estPasiRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstPasiModel no encontrado con id: " + id));

        // Validar unicidad si cambia estante o pasillo
        boolean cambioClave = !ep.getEstante().getIdEstante().equals(dto.getIdEstante())
                || !ep.getPasillo().getIdPasillo().equals(dto.getIdPasillo());

        if (cambioClave && estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(
                dto.getIdEstante(), dto.getIdPasillo())) {
            throw new IllegalArgumentException(
                    "El estante " + dto.getIdEstante() + " ya está asignado al pasillo " + dto.getIdPasillo());
        }

        EstanteModel estante = estanteRepository.findById(dto.getIdEstante())
                .orElseThrow(() -> new EntityNotFoundException("EstanteModel no encontrado con id: " + dto.getIdEstante()));
        PasilloModel pasillo = pasilloRepository.findById(dto.getIdPasillo())
                .orElseThrow(() -> new EntityNotFoundException("PasilloModel no encontrado con id: " + dto.getIdPasillo()));

        ep.setEstante(estante);
        ep.setPasillo(pasillo);
        ep.setPosicion(dto.getPosicion());
        ep.setNumeroFila(dto.getNumeroFila());
        if (dto.getOcupacionPct() != null) ep.setOcupacionPct(dto.getOcupacionPct());
        if (dto.getHabilitada() != null) ep.setHabilitada(dto.getHabilitada());
        ep.setObservaciones(dto.getObservaciones());

        EstPasiModel actualizado = estPasiRepository.save(ep);
        eventProducer.publishUbicacionActualizada(toUbicacionEvent(actualizado, "ACTUALIZADA"));
        return toResponseDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando EstPasiModel con id: {}", id);
        EstPasiModel ep = estPasiRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EstPasiModel no encontrado con id: " + id));
        estPasiRepository.deleteById(id);
        // Publicar evento de eliminación con datos antes de borrar
        eventProducer.publishUbicacionActualizada(toUbicacionEvent(ep, "ELIMINADA"));
    }

    private EstPasiResponseDTO toResponseDTO(EstPasiModel ep) {
        EstanteModel e = ep.getEstante();
        PasilloModel p = ep.getPasillo();
        return EstPasiResponseDTO.builder()
                .idEstPasi(ep.getIdEstPasi())
                .idEstante(e.getIdEstante())
                .codigoEstante(e.getCodigo())
                .descripcionEstante(e.getDescripcion())
                .numNiveles(e.getNumNiveles())
                .idPasillo(p.getIdPasillo())
                .codigoPasillo(p.getCodigo())
                .descripcionPasillo(p.getDescripcion())
                .idBodega(p.getBodega().getIdBodega())
                .nombreBodega(p.getBodega().getNombre())
                .posicion(ep.getPosicion())
                .numeroFila(ep.getNumeroFila())
                .ocupacionPct(ep.getOcupacionPct())
                .habilitada(ep.getHabilitada())
                .observaciones(ep.getObservaciones())
                .fechaAsignacion(ep.getFechaAsignacion())
                .fechaActualizacion(ep.getFechaActualizacion())
                .build();
    }

    private UbicacionActualizadaEvent toUbicacionEvent(EstPasiModel ep, String tipo) {
        return new UbicacionActualizadaEvent(
                ep.getIdEstPasi(),
                ep.getEstante().getIdEstante(),
                ep.getEstante().getCodigo(),
                ep.getPasillo().getIdPasillo(),
                ep.getPasillo().getCodigo(),
                ep.getPasillo().getBodega().getIdBodega(),
                ep.getPasillo().getBodega().getNombre(),
                ep.getOcupacionPct(),
                ep.getHabilitada(),
                tipo,
                LocalDateTime.now());
    }
}
