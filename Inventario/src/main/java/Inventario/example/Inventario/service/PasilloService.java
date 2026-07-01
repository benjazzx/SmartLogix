package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.PasilloRequestDTO;
import Inventario.example.Inventario.dto.PasilloResponseDTO;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.PasilloRepository;
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
public class PasilloService {

    private final PasilloRepository pasilloRepository;
    private final BodegaRepository bodegaRepository;

    @Transactional(readOnly = true)
    public List<PasilloResponseDTO> listarTodos() {
        return pasilloRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PasilloResponseDTO> listarPorBodega(Long idBodega) {
        log.info("Listando pasillos de la bodega: {}", idBodega);
        return pasilloRepository.findByBodegaOrdenados(idBodega).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PasilloResponseDTO obtenerPorId(Long id) {
        log.info("Obteniendo pasillo con id: {}", id);
        PasilloModel pasillo = pasilloRepository.findByIdWithEstantes(id)
                .orElseThrow(() -> new EntityNotFoundException("PasilloModel no encontrado con id: " + id));
        return toResponseDTO(pasillo);
    }

    @Transactional
    public PasilloResponseDTO crear(PasilloRequestDTO dto) {
        log.info("Creando pasillo {} en bodega {}", dto.getCodigo(), dto.getIdBodega());
        BodegaModel bodega = bodegaRepository.findById(dto.getIdBodega())
                .orElseThrow(() -> new EntityNotFoundException("BodegaModel no encontrada con id: " + dto.getIdBodega()));

        if (pasilloRepository.existsByCodigoIgnoreCaseAndBodega_IdBodega(dto.getCodigo(), dto.getIdBodega())) {
            throw new IllegalArgumentException(
                    "Ya existe un pasillo con código '" + dto.getCodigo() + "' en esa bodega");
        }

        PasilloModel pasillo = PasilloModel.builder()
                .codigo(dto.getCodigo())
                .descripcion(dto.getDescripcion())
                .numeroOrden(dto.getNumeroOrden())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .bodega(bodega)
                .build();

        PasilloModel guardado = pasilloRepository.save(pasillo);
        log.info("PasilloModel creado con id: {}", guardado.getIdPasillo());
        return toResponseDTO(guardado);
    }

    @Transactional
    public PasilloResponseDTO actualizar(Long id, PasilloRequestDTO dto) {
        log.info("Actualizando pasillo con id: {}", id);
        PasilloModel pasillo = pasilloRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PasilloModel no encontrado con id: " + id));

        BodegaModel bodega = bodegaRepository.findById(dto.getIdBodega())
                .orElseThrow(() -> new EntityNotFoundException("BodegaModel no encontrada con id: " + dto.getIdBodega()));

        // Validar código único solo si cambió el código o la bodega
        boolean codigoCambio = !pasillo.getCodigo().equalsIgnoreCase(dto.getCodigo())
                || !pasillo.getBodega().getIdBodega().equals(dto.getIdBodega());
        if (codigoCambio && pasilloRepository.existsByCodigoIgnoreCaseAndBodega_IdBodega(dto.getCodigo(), dto.getIdBodega())) {
            throw new IllegalArgumentException(
                    "Ya existe un pasillo con código '" + dto.getCodigo() + "' en esa bodega");
        }

        pasillo.setCodigo(dto.getCodigo());
        pasillo.setDescripcion(dto.getDescripcion());
        pasillo.setNumeroOrden(dto.getNumeroOrden());
        pasillo.setBodega(bodega);
        if (dto.getActivo() != null) pasillo.setActivo(dto.getActivo());

        return toResponseDTO(pasilloRepository.save(pasillo));
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando (lógico) pasillo con id: {}", id);
        PasilloModel pasillo = pasilloRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PasilloModel no encontrado con id: " + id));
        pasillo.setActivo(false);
        pasilloRepository.save(pasillo);
    }

    private PasilloResponseDTO toResponseDTO(PasilloModel p) {
        return PasilloResponseDTO.builder()
                .idPasillo(p.getIdPasillo())
                .codigo(p.getCodigo())
                .descripcion(p.getDescripcion())
                .numeroOrden(p.getNumeroOrden())
                .activo(p.getActivo())
                .idBodega(p.getBodega().getIdBodega())
                .nombreBodega(p.getBodega().getNombre())
                .totalEstantes(p.getEstantes() != null ? p.getEstantes().size() : 0)
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .build();
    }
}
