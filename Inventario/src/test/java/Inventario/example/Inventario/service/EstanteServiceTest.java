package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.EstanteRequestDTO;
import Inventario.example.Inventario.dto.EstanteResponseDTO;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.repository.EstanteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EstanteServiceTest {

    @InjectMocks
    private EstanteService estanteService;

    @Mock
    private EstanteRepository estanteRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private EstanteModel estanteSample() {
        return EstanteModel.builder()
                .idEstante(1L)
                .codigo("EST-001")
                .descripcion("Estante principal")
                .numNiveles(5)
                .capacidadPorNivel(100.0)
                .activo(true)
                .pasillos(new ArrayList<>())
                .build();
    }

    private EstanteRequestDTO dtoSample() {
        EstanteRequestDTO dto = new EstanteRequestDTO();
        dto.setCodigo("EST-001");
        dto.setDescripcion("Estante principal");
        dto.setNumNiveles(5);
        dto.setCapacidadPorNivel(100.0);
        dto.setActivo(true);
        return dto;
    }

    @Test
    void listarTodos_debeRetornarListaCompleta() {
        when(estanteRepository.findAll()).thenReturn(List.of(estanteSample()));

        List<EstanteResponseDTO> result = estanteService.listarTodos();

        assertEquals(1, result.size());
        assertEquals("EST-001", result.get(0).getCodigo());
    }

    @Test
    void listarTodos_sinEstantes_debeRetornarListaVacia() {
        when(estanteRepository.findAll()).thenReturn(List.of());

        List<EstanteResponseDTO> result = estanteService.listarTodos();

        assertTrue(result.isEmpty());
    }

    @Test
    void listarActivos_debeRetornarSoloActivos() {
        when(estanteRepository.findByActivoTrue()).thenReturn(List.of(estanteSample()));

        List<EstanteResponseDTO> result = estanteService.listarActivos();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getActivo());
    }

    @Test
    void obtenerPorId_existente_debeRetornarEstante() {
        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estanteSample()));

        EstanteResponseDTO result = estanteService.obtenerPorId(1L);

        assertEquals(1L, result.getIdEstante());
        assertEquals("EST-001", result.getCodigo());
        assertEquals(500.0, result.getCapacidadTotal());
    }

    @Test
    void obtenerPorId_noExistente_debeLanzarEntityNotFoundException() {
        when(estanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estanteService.obtenerPorId(99L));
    }

    @Test
    void listarPorPasillo_debeRetornarEstantesDelPasillo() {
        when(estanteRepository.findByPasilloId(10L)).thenReturn(List.of(estanteSample()));

        List<EstanteResponseDTO> result = estanteService.listarPorPasillo(10L);

        assertEquals(1, result.size());
    }

    @Test
    void listarPorBodega_debeRetornarEstantesDeLaBodega() {
        when(estanteRepository.findByBodegaId(5L)).thenReturn(List.of(estanteSample()));

        List<EstanteResponseDTO> result = estanteService.listarPorBodega(5L);

        assertEquals(1, result.size());
    }

    @Test
    void crear_conCodigoNuevo_debeCrearEstante() {
        EstanteRequestDTO dto = dtoSample();
        dto.setCodigo("EST-NEW");
        EstanteModel saved = estanteSample();
        saved.setCodigo("EST-NEW");
        saved.setIdEstante(2L);

        when(estanteRepository.existsByCodigoIgnoreCase("EST-NEW")).thenReturn(false);
        when(estanteRepository.save(any())).thenReturn(saved);

        EstanteResponseDTO result = estanteService.crear(dto);

        assertEquals("EST-NEW", result.getCodigo());
        verify(estanteRepository, times(1)).save(any());
    }

    @Test
    void crear_conCodigoDuplicado_debeLanzarIllegalArgumentException() {
        EstanteRequestDTO dto = dtoSample();
        when(estanteRepository.existsByCodigoIgnoreCase("EST-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> estanteService.crear(dto));
        verify(estanteRepository, never()).save(any());
    }

    @Test
    void crear_sinActivo_debeUsarActivoTrue() {
        EstanteRequestDTO dto = dtoSample();
        dto.setCodigo("EST-002");
        dto.setActivo(null);

        EstanteModel saved = estanteSample();
        saved.setCodigo("EST-002");

        when(estanteRepository.existsByCodigoIgnoreCase("EST-002")).thenReturn(false);
        when(estanteRepository.save(any())).thenReturn(saved);

        estanteService.crear(dto);

        verify(estanteRepository, times(1)).save(any());
    }

    @Test
    void actualizar_existente_conCodigoNuevo_debeActualizar() {
        EstanteModel estante = estanteSample();
        EstanteRequestDTO dto = dtoSample();
        dto.setCodigo("EST-999");

        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.existsByCodigoIgnoreCase("EST-999")).thenReturn(false);
        when(estanteRepository.save(any())).thenReturn(estante);

        EstanteResponseDTO result = estanteService.actualizar(1L, dto);

        assertNotNull(result);
        verify(estanteRepository, times(1)).save(any());
    }

    @Test
    void actualizar_mismoCodigoPropioEstante_debeActualizar() {
        EstanteModel estante = estanteSample();
        EstanteRequestDTO dto = dtoSample();

        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);

        EstanteResponseDTO result = estanteService.actualizar(1L, dto);

        assertNotNull(result);
    }

    @Test
    void actualizar_noExistente_debeLanzarEntityNotFoundException() {
        when(estanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estanteService.actualizar(99L, dtoSample()));
    }

    @Test
    void actualizar_codigoDuplicadoOtroEstante_debeLanzarExcepcion() {
        EstanteModel estante = estanteSample();
        EstanteRequestDTO dto = dtoSample();
        dto.setCodigo("EST-OTRO");

        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.existsByCodigoIgnoreCase("EST-OTRO")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> estanteService.actualizar(1L, dto));
    }

    @Test
    void eliminar_existente_debeDesactivarEstante() {
        EstanteModel estante = estanteSample();
        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);

        estanteService.eliminar(1L);

        assertFalse(estante.getActivo());
        verify(estanteRepository, times(1)).save(estante);
    }

    @Test
    void eliminar_noExistente_debeLanzarEntityNotFoundException() {
        when(estanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estanteService.eliminar(99L));
        verify(estanteRepository, never()).save(any());
    }
}
