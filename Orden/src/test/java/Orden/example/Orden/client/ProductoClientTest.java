package Orden.example.Orden.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "java:S100"})
class ProductoClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private ProductoClient productoClient;

    private static final String PRODUCTO_URL = "http://localhost:8085";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productoClient, "productoUrl", PRODUCTO_URL);
    }

    @Test
    void getProducto_productoEncontrado_retornaMap() {
        UUID productoId = UUID.randomUUID();
        Map<String, Object> productoData = Map.of("nombre", "Laptop", "precio", 999.99);
        when(circuitBreakerFactory.create("productoClient")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(PRODUCTO_URL + "/api/productos/" + productoId, Map.class))
                .thenReturn(productoData);

        Map<String, Object> result = productoClient.getProducto(productoId);

        assertNotNull(result);
        assertEquals("Laptop", result.get("nombre"));
    }

    @Test
    void getProducto_circuitBreakerActiva_retornaNull() {
        UUID productoId = UUID.randomUUID();
        when(circuitBreakerFactory.create("productoClient")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, Map<String, Object>> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("servicio caído"));
                });

        Map<String, Object> result = productoClient.getProducto(productoId);

        assertNull(result);
    }

    @Test
    void extraerNombre_mapConNombre_retornaNombre() {
        Map<String, Object> data = Map.of("nombre", "Caja Grande", "precio", 10.0);
        assertEquals("Caja Grande", ProductoClient.extraerNombre(data));
    }

    @Test
    void extraerNombre_mapNulo_retornaNull() {
        assertNull(ProductoClient.extraerNombre(null));
    }

    @Test
    void extraerNombre_mapSinNombre_retornaNull() {
        Map<String, Object> data = Map.of("precio", 10.0);
        assertNull(ProductoClient.extraerNombre(data));
    }

    @Test
    void extraerNombre_valorNombreNoEsString_retornaNull() {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", 123);
        assertNull(ProductoClient.extraerNombre(data));
    }

    @Test
    void extraerPrecio_valorDouble_retornaBigDecimal() {
        Map<String, Object> data = Map.of("precio", 999.99);
        BigDecimal result = ProductoClient.extraerPrecio(data);
        assertNotNull(result);
        assertEquals(0, result.compareTo(BigDecimal.valueOf(999.99)));
    }

    @Test
    void extraerPrecio_valorInteger_retornaBigDecimal() {
        Map<String, Object> data = Map.of("precio", 500);
        BigDecimal result = ProductoClient.extraerPrecio(data);
        assertNotNull(result);
        assertEquals(0, result.compareTo(BigDecimal.valueOf(500.0)));
    }

    @Test
    void extraerPrecio_mapNulo_retornaNull() {
        assertNull(ProductoClient.extraerPrecio(null));
    }

    @Test
    void extraerPrecio_sinPrecio_retornaNull() {
        Map<String, Object> data = Map.of("nombre", "Producto");
        assertNull(ProductoClient.extraerPrecio(data));
    }
}
