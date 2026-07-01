package Inventario.example.Inventario.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
class ProductoClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private ProductoClient productoClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productoClient = new ProductoClient();
        ReflectionTestUtils.setField(productoClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(productoClient, "circuitBreakerFactory", circuitBreakerFactory);
        ReflectionTestUtils.setField(productoClient, "productoUrl", "http://producto-service:8085");
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    @Test
    void existeProducto_productoEncontrado_retornaTrue() {
        UUID productoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(contains(productoId.toString()), eq(Map.class)))
                .thenReturn(Map.of("id", productoId.toString()));

        assertTrue(productoClient.existeProducto(productoId));
    }

    @Test
    void existeProducto_productoNull_retornaFalse() {
        UUID productoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        assertFalse(productoClient.existeProducto(productoId));
    }

    @Test
    void existeProducto_circuitAbierto_retornaTrue() {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, Boolean> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("timeout"));
                });

        assertTrue(productoClient.existeProducto(UUID.randomUUID()));
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    void decrementarStock_exitoso_retornaTrue() {
        UUID productoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PATCH), isNull(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertTrue(productoClient.decrementarStock(productoId, 5));
    }

    @Test
    void decrementarStock_circuitAbierto_retornaFalse() {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, Boolean> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("service down"));
                });

        assertFalse(productoClient.decrementarStock(UUID.randomUUID(), 3));
    }
}
