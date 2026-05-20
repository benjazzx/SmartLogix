package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.EstPasiRequestDTO;
import Inventario.example.Inventario.dto.EstPasiResponseDTO;
import Inventario.example.Inventario.service.EstPasiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class EstPasiControllerTest {

    @InjectMocks
    private EstPasiController controller;

    @Mock
    private EstPasiService estPasiService;

    private EstPasiResponseDTO sampleDto;
    private EstPasiRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleDto = EstPasiResponseDTO.builder()
                .idEstPasi(1L).idEstante(1L).idPasillo(2L)
                .idBodega(1L).posicion("A1").habilitada(true)
                .ocupacionPct(45.0).build();
        sampleRequest = EstPasiRequestDTO.builder()
                .idEstante(1L).idPasillo(2L).posicion("A1")
                .habilitada(true).ocupacionPct(45.0).build();
    }

    @Test
    void listarTodos_retornaListaOk() {
        when(estPasiService.listarTodos()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstPasiResponseDTO>> resp = controller.listarTodos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void obtenerPorId_registroExistente_retornaOk() {
        when(estPasiService.obtenerPorId(1L)).thenReturn(sampleDto);

        ResponseEntity<EstPasiResponseDTO> resp = controller.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("A1", resp.getBody().getPosicion());
    }

    @Test
    void listarPorPasillo_retornaListaOk() {
        when(estPasiService.listarPorPasillo(2L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstPasiResponseDTO>> resp = controller.listarPorPasillo(2L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void listarPorEstante_retornaListaOk() {
        when(estPasiService.listarPorEstante(1L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstPasiResponseDTO>> resp = controller.listarPorEstante(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listarPorBodega_retornaListaOk() {
        when(estPasiService.listarPorBodega(1L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<EstPasiResponseDTO>> resp = controller.listarPorBodega(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void calcularOcupacionBodega_retornaOk() {
        when(estPasiService.calcularOcupacionPromedioBodega(1L)).thenReturn(60.0);

        ResponseEntity<Map<String, Double>> resp = controller.calcularOcupacionBodega(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(60.0, resp.getBody().get("ocupacionPromedioPct"));
    }

    @Test
    void crear_datosValidos_retornaCreated() {
        when(estPasiService.crear(sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<EstPasiResponseDTO> resp = controller.crear(sampleRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void actualizar_datosValidos_retornaOk() {
        when(estPasiService.actualizar(1L, sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<EstPasiResponseDTO> resp = controller.actualizar(1L, sampleRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getIdEstPasi());
    }

    @Test
    void eliminar_registroExistente_retornaNoContent() {
        doNothing().when(estPasiService).eliminar(1L);

        ResponseEntity<Void> resp = controller.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(estPasiService).eliminar(1L);
    }
}
