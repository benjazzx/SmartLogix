package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.EstPasiRequestDTO;
import Inventario.example.Inventario.dto.EstPasiResponseDTO;
import Inventario.example.Inventario.messaging.InventarioEventProducer;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
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
import static org.mockito.Mockito.*;

class EstPasiServiceTest {

    @InjectMocks
    private EstPasiService estPasiService;

    @Mock
    private EstPasiRepository estPasiRepository;

    @Mock
    private EstanteRepository estanteRepository;

    @Mock
    private PasilloRepository pasilloRepository;

    @Mock
    private InventarioEventProducer eventProducer;

    private BodegaModel bodega;
    private PasilloModel pasillo;
    private EstanteModel estante;
    private EstPasiModel estPasi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bodega = BodegaModel.builder()
                .idBodega(1L)
                .nombre("Bodega Central")
                .activa(true)
                .pasillos(new ArrayList<>())
                .build();

        pasillo = PasilloModel.builder()
                .idPasillo(1L)
                .codigo("A1")
                .descripcion("Pasillo principal")
                .activo(true)
                .bodega(bodega)
                .estantes(new ArrayList<>())
                .build();

        estante = EstanteModel.builder()
                .idEstante(1L)
                .codigo("E001")
                .descripcion("Estante frontal")
                .numNiveles(4)
                .activo(true)
                .pasillos(new ArrayList<>())
                .build();

        estPasi = EstPasiModel.builder()
                .idEstPasi(1L)
                .estante(estante)
                .pasillo(pasillo)
                .posicion("IZQUIERDA")
                .numeroFila(1)
                .ocupacionPct(50.0)
                .habilitada(true)
                .build();
    }

    @Test
    void listarTodos_retornaLista() {
        when(estPasiRepository.findAll()).thenReturn(List.of(estPasi));

        List<EstPasiResponseDTO> result = estPasiService.listarTodos();

        assertEquals(1, result.size());
        assertEquals("E001", result.get(0).getCodigoEstante());
        assertEquals("A1", result.get(0).getCodigoPasillo());
    }

    @Test
    void listarTodos_listaVacia() {
        when(estPasiRepository.findAll()).thenReturn(List.of());

        assertTrue(estPasiService.listarTodos().isEmpty());
    }

    @Test
    void listarPorPasillo_retornaLista() {
        when(estPasiRepository.findByPasilloOrdenado(1L)).thenReturn(List.of(estPasi));

        List<EstPasiResponseDTO> result = estPasiService.listarPorPasillo(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getIdPasillo());
    }

    @Test
    void listarPorEstante_retornaLista() {
        when(estPasiRepository.findByEstante_IdEstante(1L)).thenReturn(List.of(estPasi));

        List<EstPasiResponseDTO> result = estPasiService.listarPorEstante(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getIdEstante());
    }

    @Test
    void listarPorBodega_retornaLista() {
        when(estPasiRepository.findByBodegaId(1L)).thenReturn(List.of(estPasi));

        List<EstPasiResponseDTO> result = estPasiService.listarPorBodega(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getIdBodega());
    }

    @Test
    void obtenerPorId_encontrado() {
        when(estPasiRepository.findById(1L)).thenReturn(Optional.of(estPasi));

        EstPasiResponseDTO result = estPasiService.obtenerPorId(1L);

        assertEquals(1L, result.getIdEstPasi());
        assertEquals("IZQUIERDA", result.getPosicion());
    }

    @Test
    void obtenerPorId_noEncontrado_lanzaExcepcion() {
        when(estPasiRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estPasiService.obtenerPorId(99L));
    }

    @Test
    void calcularOcupacionPromedioBodega_conDatos() {
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(1L)).thenReturn(75.0);

        Double result = estPasiService.calcularOcupacionPromedioBodega(1L);

        assertEquals(75.0, result);
    }

    @Test
    void calcularOcupacionPromedioBodega_sinDatos_retornaCero() {
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(1L)).thenReturn(null);

        Double result = estPasiService.calcularOcupacionPromedioBodega(1L);

        assertEquals(0.0, result);
    }

    @Test
    void crear_exitoso() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder()
                .idEstante(1L)
                .idPasillo(1L)
                .posicion("DERECHA")
                .numeroFila(2)
                .ocupacionPct(0.0)
                .habilitada(true)
                .build();

        when(estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(1L, 1L)).thenReturn(false);
        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(pasilloRepository.findById(1L)).thenReturn(Optional.of(pasillo));
        when(estPasiRepository.save(any(EstPasiModel.class))).thenReturn(estPasi);
        doNothing().when(eventProducer).publishUbicacionActualizada(any());

        EstPasiResponseDTO result = estPasiService.crear(dto);

        assertNotNull(result);
        verify(estPasiRepository).save(any(EstPasiModel.class));
        verify(eventProducer).publishUbicacionActualizada(any());
    }

    @Test
    void crear_yaExiste_lanzaExcepcion() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder()
                .idEstante(1L)
                .idPasillo(1L)
                .build();

        when(estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(1L, 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> estPasiService.crear(dto));
        verify(estPasiRepository, never()).save(any());
    }

    @Test
    void crear_estanteNoExiste_lanzaExcepcion() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder()
                .idEstante(99L)
                .idPasillo(1L)
                .build();

        when(estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(99L, 1L)).thenReturn(false);
        when(estanteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estPasiService.crear(dto));
    }

    @Test
    void actualizar_exitoso() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder()
                .idEstante(1L)
                .idPasillo(1L)
                .posicion("CENTRO")
                .numeroFila(3)
                .ocupacionPct(80.0)
                .habilitada(true)
                .build();

        EstPasiModel actualizado = EstPasiModel.builder()
                .idEstPasi(1L)
                .estante(estante)
                .pasillo(pasillo)
                .posicion("CENTRO")
                .ocupacionPct(80.0)
                .habilitada(true)
                .build();

        when(estPasiRepository.findById(1L)).thenReturn(Optional.of(estPasi));
        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(pasilloRepository.findById(1L)).thenReturn(Optional.of(pasillo));
        when(estPasiRepository.save(any(EstPasiModel.class))).thenReturn(actualizado);
        doNothing().when(eventProducer).publishUbicacionActualizada(any());

        EstPasiResponseDTO result = estPasiService.actualizar(1L, dto);

        assertNotNull(result);
        verify(eventProducer).publishUbicacionActualizada(any());
    }

    @Test
    void actualizar_noEncontrado_lanzaExcepcion() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder().idEstante(1L).idPasillo(1L).build();
        when(estPasiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estPasiService.actualizar(99L, dto));
    }

    @Test
    void actualizar_cambioClave_duplicado_lanzaExcepcion() {
        EstPasiRequestDTO dto = EstPasiRequestDTO.builder()
                .idEstante(2L)
                .idPasillo(2L)
                .build();

        when(estPasiRepository.findById(1L)).thenReturn(Optional.of(estPasi));
        when(estPasiRepository.existsByEstante_IdEstanteAndPasillo_IdPasillo(2L, 2L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> estPasiService.actualizar(1L, dto));
    }

    @Test
    void eliminar_exitoso() {
        when(estPasiRepository.findById(1L)).thenReturn(Optional.of(estPasi));
        doNothing().when(estPasiRepository).deleteById(1L);
        doNothing().when(eventProducer).publishUbicacionActualizada(any());

        estPasiService.eliminar(1L);

        verify(estPasiRepository).deleteById(1L);
        verify(eventProducer).publishUbicacionActualizada(any());
    }

    @Test
    void eliminar_noEncontrado_lanzaExcepcion() {
        when(estPasiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> estPasiService.eliminar(99L));
    }
}
