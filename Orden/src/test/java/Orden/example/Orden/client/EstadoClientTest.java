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

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class EstadoClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private EstadoClient estadoClient;

    private static final String ESTADO_URL = "http://localhost:8086";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(estadoClient, "estadoUrl", ESTADO_URL);
        when(circuitBreakerFactory.create("estadoClient")).thenReturn(circuitBreaker);
    }

    @Test
    void existeEstado_respuestaNoNula_retornaTrue() {
        UUID estadoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(
                ESTADO_URL + "/api/estados/" + estadoId, Map.class))
                .thenReturn(Map.of("id", estadoId.toString()));

        assertTrue(estadoClient.existeEstado(estadoId));
    }

    @Test
    void existeEstado_respuestaNula_retornaFalse() {
        UUID estadoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(
                ESTADO_URL + "/api/estados/" + estadoId, Map.class))
                .thenReturn(null);

        assertFalse(estadoClient.existeEstado(estadoId));
    }

    @Test
    void existeEstado_circuitBreakerActiva_retornaTrue() {
        UUID estadoId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, Boolean> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("servicio caído"));
                });

        assertTrue(estadoClient.existeEstado(estadoId));
    }
}
