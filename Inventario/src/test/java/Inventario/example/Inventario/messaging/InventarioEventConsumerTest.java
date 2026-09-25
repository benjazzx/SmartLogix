package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenCreadaEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionChangedEvent;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
import Inventario.example.Inventario.service.AlertaBodegaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class InventarioEventConsumerTest {

    private static final String NOMBRE_PRODUCTO = "Producto Test";
    private static final String NOMBRE_USUARIO  = "Test User";
    private static final String KEY_PRODUCTO_ID = "productoId";

    @InjectMocks
    private InventarioEventConsumer inventarioEventConsumer;

    @Mock private EstanteRepository estanteRepository;
    @Mock private EstPasiRepository estPasiRepository;
    @Mock private AlertaBodegaService alertaBodegaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(inventarioEventConsumer, "capacidadDefault", 1000);
        ReflectionTestUtils.setField(inventarioEventConsumer, "umbralAlerta", 80.0);
    }

    // ── onOrdenCreada ────────────────────────────────────────────────────────────
    // Orden ya reserva el stock de forma síncrona y atómica al crear el pedido (ver
    // OrdenService.createOrden en el servicio Orden). Este consumer solo deja traza de que
    // la orden llegó — NO debe volver a tocar stock en Producto, o lo descontaría dos veces.

    @Test
    void onOrdenCreada_conDetalles_noFalla() {
        UUID productoId = UUID.randomUUID();
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(productoId, 3, NOMBRE_PRODUCTO, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onOrdenCreada_detalleConProductoIdNulo_ignoraItem() {
        OrdenCreadaEvent.DetalleDto detalle = new OrdenCreadaEvent.DetalleDto(null, 1, null, null);
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of(detalle));

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onOrdenCreada_sinDetalles_noFalla() {
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, List.of());

        Consumer<OrdenCreadaEvent> consumer = inventarioEventConsumer.onOrdenCreada();
        assertDoesNotThrow(() -> consumer.accept(evento));
    }

    @Test
    void onOrdenCreada_detallesNulo_noFalla() {
        OrdenCreadaEvent evento = new OrdenCreadaEvent(1L, UUID.randomUUID(), NOMBRE_USUARIO, null, null, null);

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

    // ── onProductoUbicacionChanged ───────────────────────────────────────────────

    @Test
    void onProductoUbicacionChanged_stockBajoUmbral_noGeneraAlerta() {
        EstanteModel estante = EstanteModel.builder()
                .idEstante(1L).codigo("EST-001").capacidadTotal(1000).stockActual(100).build();

        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);

        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(UUID.randomUUID().toString(), 1L, 1);
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        consumer.accept(event);

        verify(estanteRepository).save(any());
        verify(alertaBodegaService, never()).crearAlertaSiNoDuplicada(any(), any(), any(), anyDouble());
    }

    @Test
    void onProductoUbicacionChanged_ocupacionSobreUmbral_creaAlerta() {
        EstanteModel estante = EstanteModel.builder()
                .idEstante(1L).codigo("EST-001").capacidadTotal(1000).stockActual(799).build();

        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);
        when(estPasiRepository.findBodegaIdByEstanteId(1L)).thenReturn(Optional.of(5L));

        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(UUID.randomUUID().toString(), 1L, 1);
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        consumer.accept(event);

        verify(alertaBodegaService).crearAlertaSiNoDuplicada(eq(1L), eq("EST-001"), eq(5L), anyDouble());
    }

    @Test
    void onProductoUbicacionChanged_eventoDuplicado_noProcesa() {
        String eventId = UUID.randomUUID().toString();
        EstanteModel estante = EstanteModel.builder()
                .idEstante(1L).codigo("EST-001").capacidadTotal(1000).stockActual(0).build();
        when(estanteRepository.findById(1L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);

        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(eventId, 1L, 1);
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        consumer.accept(event);
        consumer.accept(event);

        verify(estanteRepository, times(1)).save(any());
    }

    @Test
    void onProductoUbicacionChanged_estanteNoEncontrado_noFalla() {
        when(estanteRepository.findById(99L)).thenReturn(Optional.empty());

        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(UUID.randomUUID().toString(), 99L, 1);
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        assertDoesNotThrow(() -> consumer.accept(event));

        verify(estanteRepository, never()).save(any());
    }

    @Test
    void onProductoUbicacionChanged_eventoNulo_noFalla() {
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        assertDoesNotThrow(() -> consumer.accept(null));
    }

    @Test
    void onProductoUbicacionChanged_usaCapacidadDefaultCuandoEsNula() {
        EstanteModel estante = EstanteModel.builder()
                .idEstante(2L).codigo("EST-002").stockActual(900).build();

        when(estanteRepository.findById(2L)).thenReturn(Optional.of(estante));
        when(estanteRepository.save(any())).thenReturn(estante);
        when(estPasiRepository.findBodegaIdByEstanteId(2L)).thenReturn(Optional.empty());

        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(UUID.randomUUID().toString(), 2L, 1);
        Consumer<ProductoUbicacionChangedEvent> consumer = inventarioEventConsumer.onProductoUbicacionChanged();
        consumer.accept(event);

        // 901/1000 = 90.1% > 80% → debe crear alerta
        verify(alertaBodegaService).crearAlertaSiNoDuplicada(eq(2L), eq("EST-002"), eq(null), anyDouble());
    }
}
