package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.client.ProductoClient;
import Inventario.example.Inventario.dto.OrdenCreadaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class InventarioEventConsumerTest {

    private static final String NOMBRE_PRODUCTO = "Producto Test";
    private static final String NOMBRE_USUARIO  = "Test User";
    private static final String KEY_PRODUCTO_ID = "productoId";

    @InjectMocks
    private InventarioEventConsumer inventarioEventConsumer;

    @Mock
    private ProductoClient productoClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── onOrdenCreada ────────────────────────────────────────────────────────────

    @Test
    void onOrdenCreada_conDetalles_procesaProductosExistentes() {
        UUID productoId = UUID.randomUUID();
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(productoId, 3, NOMBRE_PRODUCTO, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        when(productoClient.existeProducto(productoId)).thenReturn(true);
        when(productoClient.decrementarStock(productoId, 3)).thenReturn(true);

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).existeProducto(productoId);
        verify(productoClient).decrementarStock(productoId, 3);
    }

    @Test
    void onOrdenCreada_productoNoExiste_noDecrementaStock() {
        UUID productoId = UUID.randomUUID();
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(productoId, 2, NOMBRE_PRODUCTO, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        when(productoClient.existeProducto(productoId)).thenReturn(false);

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).existeProducto(productoId);
        verify(productoClient, never()).decrementarStock(any(), anyInt());
    }

    @Test
    void onOrdenCreada_stockDecrementoFalla_registraAdvertencia() {
        UUID productoId = UUID.randomUUID();
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(productoId, 5, NOMBRE_PRODUCTO, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        when(productoClient.existeProducto(productoId)).thenReturn(true);
        when(productoClient.decrementarStock(productoId, 5)).thenReturn(false);

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).decrementarStock(productoId, 5);
    }

    @Test
    void onOrdenCreada_detalleConProductoIdNulo_ignoraItem() {
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(null, 1, null, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_sinDetalles_noInteractua() {
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of());

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_detallesNulo_noFalla() {
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, null);

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_excepcionEnCliente_noPropaga() {
        UUID productoId = UUID.randomUUID();
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(productoId, 1, NOMBRE_PRODUCTO, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        when(productoClient.existeProducto(any(UUID.class))).thenThrow(new RuntimeException("Error de red"));

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    // ── onProductoActualizado ────────────────────────────────────────────────────

    @Test
    void onProductoActualizado_eventoStockCambiado_procesaCorrectamente() {
        Map<String, Object> evento = new HashMap<>();
        evento.put(KEY_PRODUCTO_ID, "prod-123");
        evento.put("nombre", NOMBRE_PRODUCTO);
        evento.put("stock", 50);
        evento.put("estadoNombre", "activo");
        evento.put("tipoEvento", "STOCK_CAMBIADO");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_eventoDesactivado_procesaCorrectamente() {
        Map<String, Object> evento = new HashMap<>();
        evento.put(KEY_PRODUCTO_ID, "prod-456");
        evento.put("nombre", "Producto Desactivado");
        evento.put("stock", 0);
        evento.put("estadoNombre", "inactivo");
        evento.put("tipoEvento", "DESACTIVADO");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_otroTipoEvento_procesaSinAccionEspecial() {
        Map<String, Object> evento = new HashMap<>();
        evento.put(KEY_PRODUCTO_ID, "prod-789");
        evento.put("tipoEvento", "PRECIO_CAMBIADO");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_excepcionInterna_noPropaga() {
        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(null));
    }
}
