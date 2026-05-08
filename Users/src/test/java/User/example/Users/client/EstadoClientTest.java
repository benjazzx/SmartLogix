package User.example.Users.client;

import User.example.Users.dto.EstadoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstadoClientTest {

    @Mock private RestTemplate restTemplate;
    @SuppressWarnings("rawtypes")
    @Mock private CircuitBreakerFactory circuitBreakerFactory;
    @Mock private CircuitBreaker circuitBreaker;

    private EstadoClient estadoClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        estadoClient = new EstadoClient();
        ReflectionTestUtils.setField(estadoClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(estadoClient, "circuitBreakerFactory", circuitBreakerFactory);
        ReflectionTestUtils.setField(estadoClient, "estadoUrl", "http://estado-service:8085");
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEstadoById_respuestaOk_retornaEstado() {
        UUID estadoId = UUID.randomUUID();
        EstadoDto dto = mock(EstadoDto.class);

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(EstadoDto.class))).thenReturn(dto);

        EstadoDto result = estadoClient.getEstadoById(estadoId);

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEstadoById_circuitAbierto_lanzaExcepcion() {
        UUID estadoId = UUID.randomUUID();

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(1, java.util.function.Function.class)
                        .apply(new RuntimeException("service down")));

        assertThrows(RuntimeException.class, () -> estadoClient.getEstadoById(estadoId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEstadoByRol_respuestaOk_retornaEstado() {
        EstadoDto dto = mock(EstadoDto.class);

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(EstadoDto.class))).thenReturn(dto);

        EstadoDto result = estadoClient.getEstadoByRol("cliente");

        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEstadoByRol_circuitAbierto_retornaNull() {
        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(1, java.util.function.Function.class)
                        .apply(new RuntimeException("service down")));

        EstadoDto result = estadoClient.getEstadoByRol("cliente");

        assertNull(result);
    }
}
