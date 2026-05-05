package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.BodegaRequestDTO;
import Inventario.example.Inventario.dto.BodegaResponseDTO;
import Inventario.example.Inventario.messaging.InventarioEventProducer;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.repository.BodegaRepository;
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

class BodegaServiceTest {

    @InjectMocks
    private BodegaService bodegaService;

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private InventarioEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private BodegaModel bodegaSample() {
        return BodegaModel.builder()
                .idBodega(1L)
                .nombre("Bodega Central")
                .direccion("Av. Principal 123")
                .ciudad("Santiago")
                .pais("Chile")
                .capacidadTotal(500.0)
                .activa(true)
                .pasillos(new ArrayList<>())
                .build();
    }

    @Test
    void listarTodas_debeRetornarListaCompleta() {
        when(bodegaRepository.findAll()).thenReturn(List.of(bodegaSample()));

        List<BodegaResponseDTO> result = bodegaService.listarTodas();

        assertEquals(1, result.size());
        assertEquals("Bodega Central", result.get(0).getNombre());
    }

    @Test
    void listarTodas_sinBodegas_debeRetornarListaVacia() {
        when(bodegaRepository.findAll()).thenReturn(List.of());

        List<BodegaResponseDTO> result = bodegaService.listarTodas();

        assertTrue(result.isEmpty());
    }

    @Test
    void listarActivas_debeRetornarSoloBodegasActivas() {
        when(bodegaRepository.findByActivaTrue()).thenReturn(List.of(bodegaSample()));

        List<BodegaResponseDTO> result = bodegaService.listarActivas();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getActiva());
    }

    @Test
    void obtenerPorId_existente_debeRetornarBodega() {
        when(bodegaRepository.findByIdWithPasillos(1L)).thenReturn(Optional.of(bodegaSample()));

        BodegaResponseDTO result = bodegaService.obtenerPorId(1L);

        assertEquals(1L, result.getIdBodega());
        assertEquals("Bodega Central", result.getNombre());
        assertEquals("Santiago", result.getCiudad());
    }

    @Test
    void obtenerPorId_noExistente_debeLanzarEntityNotFoundException() {
        when(bodegaRepository.findByIdWithPasillos(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bodegaService.obtenerPorId(99L));
    }

    @Test
    void crear_conNombreNuevo_debeCrearBodegaYPublicarEvento() {
        BodegaRequestDTO dto = BodegaRequestDTO.builder()
                .nombre("Nueva Bodega Norte")
                .direccion("Calle 1")
                .ciudad("Valparaíso")
                .pais("Chile")
                .capacidadTotal(200.0)
                .activa(true)
                .build();

        BodegaModel saved = BodegaModel.builder()
                .idBodega(2L)
                .nombre("Nueva Bodega Norte")
                .ciudad("Valparaíso")
                .pais("Chile")
                .activa(true)
                .pasillos(new ArrayList<>())
                .build();

        when(bodegaRepository.existsByNombreIgnoreCase("Nueva Bodega Norte")).thenReturn(false);
        when(bodegaRepository.save(any())).thenReturn(saved);
        doNothing().when(eventProducer).publishBodegaActualizada(any());

        BodegaResponseDTO result = bodegaService.crear(dto);

        assertEquals("Nueva Bodega Norte", result.getNombre());
        verify(bodegaRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishBodegaActualizada(any());
    }

    @Test
    void crear_conNombreDuplicado_debeLanzarIllegalArgumentException() {
        BodegaRequestDTO dto = BodegaRequestDTO.builder().nombre("Bodega Central").build();
        when(bodegaRepository.existsByNombreIgnoreCase("Bodega Central")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> bodegaService.crear(dto));
        verify(bodegaRepository, never()).save(any());
    }

    @Test
    void actualizar_existente_conNombreNuevo_debeActualizarBodega() {
        BodegaModel bodega = bodegaSample();
        BodegaRequestDTO dto = BodegaRequestDTO.builder()
                .nombre("Bodega Sur")
                .ciudad("Concepción")
                .pais("Chile")
                .capacidadTotal(600.0)
                .activa(true)
                .build();

        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(bodegaRepository.existsByNombreIgnoreCase("Bodega Sur")).thenReturn(false);
        when(bodegaRepository.save(any())).thenReturn(bodega);
        doNothing().when(eventProducer).publishBodegaActualizada(any());

        BodegaResponseDTO result = bodegaService.actualizar(1L, dto);

        assertNotNull(result);
        verify(bodegaRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishBodegaActualizada(any());
    }

    @Test
    void actualizar_mismoNombre_debePermitirActualizacion() {
        BodegaModel bodega = bodegaSample();
        BodegaRequestDTO dto = BodegaRequestDTO.builder()
                .nombre("Bodega Central")
                .ciudad("Santiago")
                .pais("Chile")
                .activa(true)
                .build();

        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(bodegaRepository.save(any())).thenReturn(bodega);
        doNothing().when(eventProducer).publishBodegaActualizada(any());

        BodegaResponseDTO result = bodegaService.actualizar(1L, dto);

        assertNotNull(result);
    }

    @Test
    void actualizar_noExistente_debeLanzarEntityNotFoundException() {
        when(bodegaRepository.findById(99L)).thenReturn(Optional.empty());
        BodegaRequestDTO dto = BodegaRequestDTO.builder().nombre("Test").build();

        assertThrows(EntityNotFoundException.class, () -> bodegaService.actualizar(99L, dto));
    }

    @Test
    void actualizar_nombreDuplicadoDiferenteBodega_debeLanzarExcepcion() {
        BodegaModel bodega = bodegaSample();
        BodegaRequestDTO dto = BodegaRequestDTO.builder().nombre("Otra Bodega").build();

        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(bodegaRepository.existsByNombreIgnoreCase("Otra Bodega")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> bodegaService.actualizar(1L, dto));
    }

    @Test
    void eliminar_existente_debeDesactivarBodegaYPublicarEvento() {
        BodegaModel bodega = bodegaSample();
        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(bodegaRepository.save(any())).thenReturn(bodega);
        doNothing().when(eventProducer).publishBodegaActualizada(any());

        bodegaService.eliminar(1L);

        assertFalse(bodega.getActiva());
        verify(bodegaRepository, times(1)).save(bodega);
        verify(eventProducer, times(1)).publishBodegaActualizada(any());
    }

    @Test
    void eliminar_noExistente_debeLanzarEntityNotFoundException() {
        when(bodegaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bodegaService.eliminar(99L));
        verify(bodegaRepository, never()).save(any());
    }
}
