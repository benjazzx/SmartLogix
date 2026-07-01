package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.BodegaRequestDTO;
import Inventario.example.Inventario.dto.BodegaResponseDTO;
import Inventario.example.Inventario.service.BodegaService;
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
class BodegaControllerTest {

    @InjectMocks
    private BodegaController controller;

    @Mock
    private BodegaService bodegaService;

    private BodegaResponseDTO sampleDto;
    private BodegaRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleDto = BodegaResponseDTO.builder()
                .idBodega(1L).nombre("Bodega Central")
                .ciudad("Santiago").activa(true).build();
        sampleRequest = BodegaRequestDTO.builder()
                .nombre("Bodega Central").ciudad("Santiago").activa(true).build();
    }

    @Test
    void listarTodas_retornaListaOk() {
        when(bodegaService.listarTodas()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<BodegaResponseDTO>> resp = controller.listarTodas();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void listarActivas_retornaListaOk() {
        when(bodegaService.listarActivas()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<BodegaResponseDTO>> resp = controller.listarActivas();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void obtenerPorId_bodegaExistente_retornaOk() {
        when(bodegaService.obtenerPorId(1L)).thenReturn(sampleDto);

        ResponseEntity<BodegaResponseDTO> resp = controller.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Bodega Central", resp.getBody().getNombre());
    }

    @Test
    void crear_datosValidos_retornaCreated() {
        when(bodegaService.crear(sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<BodegaResponseDTO> resp = controller.crear(sampleRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void actualizar_datosValidos_retornaOk() {
        when(bodegaService.actualizar(1L, sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<BodegaResponseDTO> resp = controller.actualizar(1L, sampleRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getIdBodega());
    }

    @Test
    void eliminar_bodegaExistente_retornaNoContent() {
        doNothing().when(bodegaService).eliminar(1L);

        ResponseEntity<Void> resp = controller.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(bodegaService).eliminar(1L);
    }
}
