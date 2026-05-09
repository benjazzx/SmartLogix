package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.client.ProductoClient;
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

class InventarioEventConsumerTest {

    @InjectMocks
    private InventarioEventConsumer inventarioEventConsumer;

    @Mock
    private ProductoClient productoClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void onOrdenCreada_conDetalles_procesaProductosExistentes() {
        UUID productoId = UUID.randomUUID();

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("productoId", productoId.toString());
        detalle.put("cantidad", "3");

        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-123");
        evento.put("userId", "user-456");
        evento.put("detalles", List.of(detalle));

        when(productoClient.existeProducto(any(UUID.class))).thenReturn(true);
        when(productoClient.decrementarStock(any(UUID.class), anyInt())).thenReturn(true);

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).existeProducto(productoId);
        verify(productoClient).decrementarStock(productoId, 3);
    }

    @Test
    void onOrdenCreada_productoNoExiste_noDecrementaStock() {
        UUID productoId = UUID.randomUUID();

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("productoId", productoId.toString());
        detalle.put("cantidad", "2");

        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-123");
        evento.put("detalles", List.of(detalle));

        when(productoClient.existeProducto(any(UUID.class))).thenReturn(false);

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).existeProducto(productoId);
        verify(productoClient, never()).decrementarStock(any(), anyInt());
    }

    @Test
    void onOrdenCreada_stockDecrementoFalla_registraAdvertencia() {
        UUID productoId = UUID.randomUUID();

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("productoId", productoId.toString());
        detalle.put("cantidad", "5");

        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-789");
        evento.put("detalles", List.of(detalle));

        when(productoClient.existeProducto(any(UUID.class))).thenReturn(true);
        when(productoClient.decrementarStock(any(UUID.class), anyInt())).thenReturn(false);

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient).decrementarStock(productoId, 5);
    }

    @Test
    void onOrdenCreada_detalleConCamposNulos_ignoraItem() {
        Map<String, Object> detalleSinProducto = new HashMap<>();
        detalleSinProducto.put("productoId", null);
        detalleSinProducto.put("cantidad", "1");

        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-null");
        evento.put("detalles", List.of(detalleSinProducto));

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_sinDetalles_noInteractua() {
        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-sin-detalles");
        evento.put("detalles", List.of());

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_detallesNulo_noFalla() {
        Map<String, Object> evento = new HashMap<>();
        evento.put("ordenId", "orden-nula");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        consumer.accept(evento);

        verify(productoClient, never()).existeProducto(any());
    }

    @Test
    void onOrdenCreada_excepcionInterna_noPropaга() {
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("productoId", "uuid-invalido");
        detalle.put("cantidad", "1");

        Map<String, Object> evento = new HashMap<>();
        evento.put("detalles", List.of(detalle));

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onOrdenCreada();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_eventoStockCambiado_procesaCorrectamente() {
        Map<String, Object> evento = new HashMap<>();
        evento.put("productoId", "prod-123");
        evento.put("nombre", "Producto Test");
        evento.put("stock", 50);
        evento.put("estadoNombre", "activo");
        evento.put("tipoEvento", "STOCK_CAMBIADO");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_eventoDesactivado_procesaCorrectamente() {
        Map<String, Object> evento = new HashMap<>();
        evento.put("productoId", "prod-456");
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
        evento.put("productoId", "prod-789");
        evento.put("tipoEvento", "PRECIO_CAMBIADO");

        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onProductoActualizado_excepcionInterna_noPropaга() {
        Consumer<Map<String, Object>> consumer = inventarioEventConsumer.onProductoActualizado();
        assertDoesNotThrow(() -> consumer.accept(null));
    }
}
