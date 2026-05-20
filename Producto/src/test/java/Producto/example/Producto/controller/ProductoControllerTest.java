package Producto.example.Producto.controller;

import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class ProductoControllerTest {

    @InjectMocks
    private ProductoController controller;

    @Mock
    private ProductoService productoService;

    private UUID productoId;
    private ProductoResponseDTO sampleDto;
    private ProductoRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productoId = UUID.randomUUID();
        sampleDto = new ProductoResponseDTO();
        sampleDto.setId(productoId);
        sampleDto.setNombre("Laptop Pro");
        sampleDto.setPrecio(BigDecimal.valueOf(999.99));
        sampleDto.setStock(50);
        sampleDto.setActivo(true);

        sampleRequest = new ProductoRequestDTO();
        sampleRequest.setNombre("Laptop Pro");
        sampleRequest.setPrecio(BigDecimal.valueOf(999.99));
        sampleRequest.setStock(50);
        sampleRequest.setCategoriaId(UUID.randomUUID());
    }

    @Test
    void getAll_retornaListaProductosActivos() {
        when(productoService.getAll()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<ProductoResponseDTO>> resp = controller.getAll();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getAllIncluirInactivos_retornaListaCompleta() {
        when(productoService.getAllIncluirInactivos()).thenReturn(List.of(sampleDto));

        ResponseEntity<List<ProductoResponseDTO>> resp = controller.getAllIncluirInactivos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void getById_productoExistente_retornaOk() {
        when(productoService.getById(productoId)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp = controller.getById(productoId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Laptop Pro", resp.getBody().getNombre());
    }

    @Test
    void getByCategoria_retornaListaFiltrada() {
        UUID catId = UUID.randomUUID();
        when(productoService.getByCategoria(catId)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<ProductoResponseDTO>> resp = controller.getByCategoria(catId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void buscar_porNombre_retornaResultados() {
        when(productoService.buscarPorNombre("Laptop")).thenReturn(List.of(sampleDto));

        ResponseEntity<List<ProductoResponseDTO>> resp = controller.buscar("Laptop");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().isEmpty());
    }

    @Test
    void getBajoStock_retornaProductosBajoUmbral() {
        when(productoService.getBajoStock(10)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<ProductoResponseDTO>> resp = controller.getBajoStock(10);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void crear_datosValidos_retornaCreated() {
        when(productoService.crear(sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp = controller.crear(sampleRequest);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
    }

    @Test
    void actualizar_datosValidos_retornaOk() {
        when(productoService.actualizar(productoId, sampleRequest)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp = controller.actualizar(productoId, sampleRequest);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(productoId, resp.getBody().getId());
    }

    @Test
    void actualizarStock_stockValido_retornaOk() {
        sampleDto.setStock(100);
        when(productoService.actualizarStock(productoId, 100)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp =
                controller.actualizarStock(productoId, Map.of("stock", 100));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(100, resp.getBody().getStock());
    }

    @Test
    void actualizarStock_sinCampoStock_retorna400() {
        ResponseEntity<ProductoResponseDTO> resp =
                controller.actualizarStock(productoId, Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void actualizarStock_stockNegativo_retorna400() {
        ResponseEntity<ProductoResponseDTO> resp =
                controller.actualizarStock(productoId, Map.of("stock", -5));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void decrementarStock_cantidadValida_retornaOk() {
        when(productoService.decrementarStock(productoId, 3)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp = controller.decrementarStock(productoId, 3);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void decrementarStock_cantidadCero_retorna400() {
        ResponseEntity<ProductoResponseDTO> resp = controller.decrementarStock(productoId, 0);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void toggleActivo_cambiasEstado_retornaOk() {
        sampleDto.setActivo(false);
        when(productoService.toggleActivo(productoId)).thenReturn(sampleDto);

        ResponseEntity<ProductoResponseDTO> resp = controller.toggleActivo(productoId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertFalse(resp.getBody().getActivo());
    }

    @Test
    void desactivar_productoExistente_retornaNoContent() {
        doNothing().when(productoService).desactivar(productoId);

        ResponseEntity<Void> resp = controller.desactivar(productoId);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(productoService).desactivar(productoId);
    }
}
