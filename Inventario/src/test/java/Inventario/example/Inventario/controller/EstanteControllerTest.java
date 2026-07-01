package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.EstanteRequestDTO;
import Inventario.example.Inventario.dto.EstanteResponseDTO;
import Inventario.example.Inventario.service.EstanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class EstanteControllerTest {

    @InjectMocks
    private EstanteController controller;

    @Mock
    private EstanteService estanteService;

    private EstanteResponseDTO sampleDto;
    private EstanteRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleDto = EstanteResponseDTO.builder()
                .idEstante(1L).codigo("E-001").numNiveles(5)
                .capacidadPorNivel(100.0).activo(true).build();
        sampleRequest = EstanteRequestDTO.builder()
                .codigo("E-001").numNiveles(5).capacidadPorNivel(100.0).activo(true).build();
    }

    @Test
    void listarTodos_retornaListaOk() {
        when(estanteService.listarTodos()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstanteResponseDTO>> resp = controller.listarTodos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listarActivos_retornaListaOk() {
        when(estanteService.listarActivos()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstanteResponseDTO>> resp = controller.listarActivos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void obtenerPorId_estanteExistente_retornaOk() {
        when(estanteService.obtenerPorId(1L)).thenReturn(sampleDto);

        ResponseEntity<EstanteResponseDTO> resp = controller.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("E-001", resp.getBody().getCodigo());
    }

    @Test
    void listarPorPasillo_retornaListaOk() {
        when(estanteService.listarPorPasillo(10L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstanteResponseDTO>> resp = controller.listarPorPasillo(10L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listarPorBodega_retornaListaOk() {
        when(estanteService.listarPorBodega(5L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstanteResponseDTO>> resp = controller.listarPorBodega(5L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void crear_datosValidos_retornaCreated() {
        when(estanteService.crear(sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<EstanteResponseDTO> resp = controller.crear(sampleRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void actualizar_datosValidos_retornaOk() {
        when(estanteService.actualizar(1L, sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<EstanteResponseDTO> resp = controller.actualizar(1L, sampleRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getIdEstante());
    }

    @Test
    void eliminar_estanteExistente_retornaNoContent() {
        doNothing().when(estanteService).eliminar(1L);

        ResponseEntity<Void> resp = controller.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(estanteService).eliminar(1L);
    }
}
