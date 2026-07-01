package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.PasilloRequestDTO;
import Inventario.example.Inventario.dto.PasilloResponseDTO;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.PasilloRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasilloServiceTest {

    @InjectMocks
    private PasilloService pasilloService;

    @Mock
    private PasilloRepository pasilloRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    private BodegaModel bodega;
    private PasilloModel pasillo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bodega = BodegaModel.builder()
                .idBodega(1L)
                .nombre("Bodega Central")
                .ciudad("Santiago")
                .pais("Chile")
                .activa(true)
                .pasillos(new ArrayList<>())
                .build();

        pasillo = PasilloModel.builder()
                .idPasillo(1L)
                .codigo("A1")
                .descripcion("Pasillo principal")
                .numeroOrden(1)
                .activo(true)
                .bodega(bodega)
                .estantes(new ArrayList<>())
                .build();
    }

    @Test
    void listarTodos_retornaLista() {
        when(pasilloRepository.findAll()).thenReturn(List.of(pasillo));

        List<PasilloResponseDTO> result = pasilloService.listarTodos();

        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getCodigo());
        assertEquals("Bodega Central", result.get(0).getNombreBodega());
    }

    @Test
    void listarTodos_listaVacia() {
        when(pasilloRepository.findAll()).thenReturn(List.of());

        assertTrue(pasilloService.listarTodos().isEmpty());
    }

    @Test
    void listarPorBodega_retornaLista() {
        when(pasilloRepository.findByBodegaOrdenados(1L)).thenReturn(List.of(pasillo));

        List<PasilloResponseDTO> result = pasilloService.listarPorBodega(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getIdBodega());
    }

    @Test
    void obtenerPorId_encontrado() {
        when(pasilloRepository.findByIdWithEstantes(1L)).thenReturn(Optional.of(pasillo));

        PasilloResponseDTO result = pasilloService.obtenerPorId(1L);

        assertEquals("A1", result.getCodigo());
        assertEquals(1L, result.getIdPasillo());
    }

    @Test
    void obtenerPorId_noEncontrado_lanzaExcepcion() {
        when(pasilloRepository.findByIdWithEstantes(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> pasilloService.obtenerPorId(99L));
    }

    @Test
    void crear_exitoso() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder()
                .codigo("B2")
                .descripcion("Pasillo secundario")
                .numeroOrden(2)
                .activo(true)
                .idBodega(1L)
                .build();

        PasilloModel nuevoPasillo = PasilloModel.builder()
                .idPasillo(2L)
                .codigo("B2")
                .descripcion("Pasillo secundario")
                .numeroOrden(2)
                .activo(true)
                .bodega(bodega)
                .estantes(new ArrayList<>())
                .build();

        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(pasilloRepository.existsByCodigoIgnoreCaseAndBodega_IdBodega("B2", 1L)).thenReturn(false);
        when(pasilloRepository.save(any(PasilloModel.class))).thenReturn(nuevoPasillo);

        PasilloResponseDTO result = pasilloService.crear(dto);

        assertEquals("B2", result.getCodigo());
        verify(pasilloRepository).save(any(PasilloModel.class));
    }

    @Test
    void crear_bodegaNoExiste_lanzaExcepcion() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder()
                .codigo("B2")
                .idBodega(99L)
                .build();

        when(bodegaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> pasilloService.crear(dto));
    }

    @Test
    void crear_codigoDuplicado_lanzaExcepcion() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder()
                .codigo("A1")
                .idBodega(1L)
                .build();

        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(pasilloRepository.existsByCodigoIgnoreCaseAndBodega_IdBodega("A1", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> pasilloService.crear(dto));
        verify(pasilloRepository, never()).save(any());
    }

    @Test
    void actualizar_exitoso() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder()
                .codigo("A2")
                .descripcion("Actualizado")
                .numeroOrden(3)
                .activo(true)
                .idBodega(1L)
                .build();

        PasilloModel actualizado = PasilloModel.builder()
                .idPasillo(1L)
                .codigo("A2")
                .descripcion("Actualizado")
                .activo(true)
                .bodega(bodega)
                .estantes(new ArrayList<>())
                .build();

        when(pasilloRepository.findById(1L)).thenReturn(Optional.of(pasillo));
        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(pasilloRepository.existsByCodigoIgnoreCaseAndBodega_IdBodega("A2", 1L)).thenReturn(false);
        when(pasilloRepository.save(any(PasilloModel.class))).thenReturn(actualizado);

        PasilloResponseDTO result = pasilloService.actualizar(1L, dto);

        assertEquals("A2", result.getCodigo());
    }

    @Test
    void actualizar_mismoCodigo_noValidaDuplicado() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder()
                .codigo("A1")
                .descripcion("Sin cambio de código")
                .numeroOrden(1)
                .activo(true)
                .idBodega(1L)
                .build();

        when(pasilloRepository.findById(1L)).thenReturn(Optional.of(pasillo));
        when(bodegaRepository.findById(1L)).thenReturn(Optional.of(bodega));
        when(pasilloRepository.save(any(PasilloModel.class))).thenReturn(pasillo);

        PasilloResponseDTO result = pasilloService.actualizar(1L, dto);

        assertEquals("A1", result.getCodigo());
        verify(pasilloRepository, never()).existsByCodigoIgnoreCaseAndBodega_IdBodega(anyString(), anyLong());
    }

    @Test
    void actualizar_noEncontrado_lanzaExcepcion() {
        PasilloRequestDTO dto = PasilloRequestDTO.builder().codigo("A1").idBodega(1L).build();
        when(pasilloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> pasilloService.actualizar(99L, dto));
    }

    @Test
    void eliminar_marcaInactivo() {
        when(pasilloRepository.findById(1L)).thenReturn(Optional.of(pasillo));
        when(pasilloRepository.save(any(PasilloModel.class))).thenReturn(pasillo);

        pasilloService.eliminar(1L);

        assertFalse(pasillo.getActivo());
        verify(pasilloRepository).save(pasillo);
    }

    @Test
    void eliminar_noEncontrado_lanzaExcepcion() {
        when(pasilloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> pasilloService.eliminar(99L));
    }
}
