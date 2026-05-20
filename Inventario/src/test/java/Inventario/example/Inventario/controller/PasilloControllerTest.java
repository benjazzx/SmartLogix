package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.PasilloRequestDTO;
import Inventario.example.Inventario.dto.PasilloResponseDTO;
import Inventario.example.Inventario.service.PasilloService;
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
class PasilloControllerTest {

    @InjectMocks
    private PasilloController controller;

    @Mock
    private PasilloService pasilloService;

    private PasilloResponseDTO sampleDto;
    private PasilloRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleDto = PasilloResponseDTO.builder()
                .idPasillo(1L).codigo("P-001").activo(true)
                .idBodega(1L).nombreBodega("Bodega Central").build();
        sampleRequest = PasilloRequestDTO.builder()
                .codigo("P-001").activo(true).idBodega(1L).build();
    }

    @Test
    void listarTodos_retornaListaOk() {
        when(pasilloService.listarTodos()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<PasilloResponseDTO>> resp = controller.listarTodos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listarPorBodega_retornaListaOk() {
        when(pasilloService.listarPorBodega(1L)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<PasilloResponseDTO>> resp = controller.listarPorBodega(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void obtenerPorId_pasilloExistente_retornaOk() {
        when(pasilloService.obtenerPorId(1L)).thenReturn(sampleDto);

        ResponseEntity<PasilloResponseDTO> resp = controller.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("P-001", resp.getBody().getCodigo());
    }

    @Test
    void crear_datosValidos_retornaCreated() {
        when(pasilloService.crear(sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<PasilloResponseDTO> resp = controller.crear(sampleRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void actualizar_datosValidos_retornaOk() {
        when(pasilloService.actualizar(1L, sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<PasilloResponseDTO> resp = controller.actualizar(1L, sampleRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getIdPasillo());
    }

    @Test
    void eliminar_pasilloExistente_retornaNoContent() {
        doNothing().when(pasilloService).eliminar(1L);

        ResponseEntity<Void> resp = controller.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(pasilloService).eliminar(1L);
    }
}
