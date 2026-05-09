package Producto.example.Producto.service;

import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.messaging.ProductoEventProducer;
import Producto.example.Producto.model.CategoriaModel;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.CategoriaRepository;
import Producto.example.Producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductoServiceTest {

    @InjectMocks
    private ProductoService productoService;

    @Mock private ProductoRepository productoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private CategoriaModel categoriaSample() {
        return CategoriaModel.builder()
                .id(UUID.randomUUID())
                .nombre("Electrónica")
                .descripcion("Dispositivos electrónicos")
                .build();
    }

    private ProductoModel productoSample(CategoriaModel cat) {
        return ProductoModel.builder()
                .id(UUID.randomUUID())
                .nombre("Laptop Pro")
                .descripcion("Laptop de alta gama")
                .precio(new BigDecimal("999.99"))
                .stock(50)
                .categoria(cat)
                .estadoNombre("publicado")
                .activo(true)
                .build();
    }

    @Test
    void getAll_debeRetornarSoloProductosActivos() {
        CategoriaModel cat = categoriaSample();
        when(productoRepository.findByActivoTrue()).thenReturn(List.of(productoSample(cat)));

        List<ProductoResponseDTO> result = productoService.getAll();

        assertEquals(1, result.size());
        assertEquals("Laptop Pro", result.get(0).getNombre());
        assertTrue(result.get(0).getActivo());
    }

    @Test
    void getAllIncluirInactivos_debeRetornarTodosLosProductos() {
        CategoriaModel cat = categoriaSample();
        ProductoModel inactivo = productoSample(cat);
        inactivo.setActivo(false);
        when(productoRepository.findAll()).thenReturn(List.of(productoSample(cat), inactivo));

        List<ProductoResponseDTO> result = productoService.getAllIncluirInactivos();

        assertEquals(2, result.size());
    }

    @Test
    void getById_existente_debeRetornarProducto() {
        CategoriaModel cat = categoriaSample();
        ProductoModel producto = productoSample(cat);
        UUID id = producto.getId();
        when(productoRepository.findById(id)).thenReturn(Optional.of(producto));

        ProductoResponseDTO result = productoService.getById(id);

        assertEquals(id, result.getId());
        assertEquals("Laptop Pro", result.getNombre());
        assertEquals("Electrónica", result.getCategoriaNombre());
    }

    @Test
    void getById_noExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.getById(id));
    }

    @Test
    void getByCategoria_debeRetornarProductosDeLaCategoria() {
        CategoriaModel cat = categoriaSample();
        when(productoRepository.findByCategoria_Id(cat.getId())).thenReturn(List.of(productoSample(cat)));

        List<ProductoResponseDTO> result = productoService.getByCategoria(cat.getId());

        assertEquals(1, result.size());
    }

    @Test
    void buscarPorNombre_debeRetornarProductosCoincidentes() {
        CategoriaModel cat = categoriaSample();
        when(productoRepository.findByNombreContainingIgnoreCase("laptop")).thenReturn(List.of(productoSample(cat)));

        List<ProductoResponseDTO> result = productoService.buscarPorNombre("laptop");

        assertEquals(1, result.size());
    }

    @Test
    void getBajoStock_debeRetornarProductosBajoUmbral() {
        CategoriaModel cat = categoriaSample();
        ProductoModel bajStock = productoSample(cat);
        bajStock.setStock(5);
        when(productoRepository.findByStockLessThanEqual(10)).thenReturn(List.of(bajStock));

        List<ProductoResponseDTO> result = productoService.getBajoStock(10);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getStock());
    }

    @Test
    void crear_conDatosValidos_debeCrearProductoYPublicarEvento() {
        CategoriaModel cat = categoriaSample();
        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setNombre("Teclado Mecánico");
        dto.setDescripcion("Teclado para gaming");
        dto.setPrecio(new BigDecimal("89.99"));
        dto.setStock(30);
        dto.setCategoriaId(cat.getId());

        ProductoModel saved = productoSample(cat);
        saved.setNombre("Teclado Mecánico");

        when(categoriaRepository.findById(cat.getId())).thenReturn(Optional.of(cat));
        when(productoRepository.save(any())).thenReturn(saved);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        ProductoResponseDTO result = productoService.crear(dto);

        assertNotNull(result);
        verify(productoRepository, times(1)).save(any());
        verify(eventProducer, times(1)).publishProductoActualizado(any(), eq(ProductoActualizadoEvent.TipoEvento.CREADO));
    }

    @Test
    void crear_categoriaNoExistente_debeLanzarRuntimeException() {
        UUID catId = UUID.randomUUID();
        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setNombre("Producto Test");
        dto.setPrecio(new BigDecimal("10.00"));
        dto.setStock(5);
        dto.setCategoriaId(catId);

        when(categoriaRepository.findById(catId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.crear(dto));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void actualizar_sinCambioStock_publicaEventoACTUALIZADO() {
        CategoriaModel cat = categoriaSample();
        ProductoModel existente = productoSample(cat);
        UUID id = existente.getId();

        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setNombre("Laptop Pro V2");
        dto.setDescripcion("Versión mejorada");
        dto.setPrecio(new BigDecimal("1099.99"));
        dto.setStock(50);
        dto.setCategoriaId(cat.getId());

        when(productoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(cat.getId())).thenReturn(Optional.of(cat));
        when(productoRepository.save(any())).thenReturn(existente);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        ProductoResponseDTO result = productoService.actualizar(id, dto);

        assertNotNull(result);
        verify(eventProducer).publishProductoActualizado(any(), eq(ProductoActualizadoEvent.TipoEvento.ACTUALIZADO));
    }

    @Test
    void actualizar_conCambioStock_publicaEventoSTOCK_CAMBIADO() {
        CategoriaModel cat = categoriaSample();
        ProductoModel existente = productoSample(cat);
        UUID id = existente.getId();

        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setNombre("Laptop Pro");
        dto.setDescripcion("Laptop de alta gama");
        dto.setPrecio(new BigDecimal("999.99"));
        dto.setStock(100);
        dto.setCategoriaId(cat.getId());

        when(productoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(cat.getId())).thenReturn(Optional.of(cat));
        when(productoRepository.save(any())).thenReturn(existente);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        productoService.actualizar(id, dto);

        verify(eventProducer).publishProductoActualizado(any(), eq(ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO));
    }

    @Test
    void actualizar_productoNoExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setCategoriaId(UUID.randomUUID());
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.actualizar(id, dto));
    }

    @Test
    void actualizarStock_stockCero_debePonerEstadoSinStock() {
        CategoriaModel cat = categoriaSample();
        ProductoModel producto = productoSample(cat);
        UUID id = producto.getId();

        when(productoRepository.findById(id)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenReturn(producto);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        ProductoResponseDTO result = productoService.actualizarStock(id, 0);

        assertEquals("sin_stock", producto.getEstadoNombre());
        verify(eventProducer).publishProductoActualizado(any(), eq(ProductoActualizadoEvent.TipoEvento.STOCK_CAMBIADO));
    }

    @Test
    void actualizarStock_stockBajoUmbral_debePonerEstadoBajoStock() {
        CategoriaModel cat = categoriaSample();
        ProductoModel producto = productoSample(cat);
        UUID id = producto.getId();

        when(productoRepository.findById(id)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenReturn(producto);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        productoService.actualizarStock(id, 8);

        assertEquals("bajo_stock", producto.getEstadoNombre());
    }

    @Test
    void actualizarStock_stockNormal_debePonerEstadoPublicado() {
        CategoriaModel cat = categoriaSample();
        ProductoModel producto = productoSample(cat);
        UUID id = producto.getId();

        when(productoRepository.findById(id)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenReturn(producto);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        productoService.actualizarStock(id, 50);

        assertEquals("publicado", producto.getEstadoNombre());
    }

    @Test
    void actualizarStock_productoNoExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.actualizarStock(id, 10));
    }

    @Test
    void desactivar_existente_debeDesactivarYPublicarEventoDESACTIVADO() {
        CategoriaModel cat = categoriaSample();
        ProductoModel producto = productoSample(cat);
        UUID id = producto.getId();

        when(productoRepository.findById(id)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenReturn(producto);
        doNothing().when(eventProducer).publishProductoActualizado(any(), any());

        productoService.desactivar(id);

        assertFalse(producto.getActivo());
        assertEquals("descontinuado", producto.getEstadoNombre());
        verify(eventProducer).publishProductoActualizado(any(), eq(ProductoActualizadoEvent.TipoEvento.DESACTIVADO));
    }

    @Test
    void desactivar_productoNoExistente_debeLanzarRuntimeException() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.desactivar(id));
    }
}
