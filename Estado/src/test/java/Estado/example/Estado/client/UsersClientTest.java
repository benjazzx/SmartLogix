package Estado.example.Estado.client;

import Estado.example.Estado.Client.UsersClient;
import Estado.example.Estado.Dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
class UsersClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private UsersClient usersClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usersClient = new UsersClient();
        ReflectionTestUtils.setField(usersClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(usersClient, "circuitBreakerFactory", circuitBreakerFactory);
        ReflectionTestUtils.setField(usersClient, "usersUrl", "http://users-service:8082");
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    @Test
    void getUsuariosByEstado_exitoso_retornaLista() {
        UUID estadoId = UUID.randomUUID();
        UserDto userDto = new UserDto();
        List<UserDto> expected = List.of(userDto);

        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Supplier<List<UserDto>> supplier = inv.getArgument(0);
                    return supplier.get();
                });
        when(restTemplate.exchange(
                contains(estadoId.toString()),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(expected));

        List<UserDto> result = usersClient.getUsuariosByEstado(estadoId);

        assertEquals(1, result.size());
    }

    @Test
    void getUsuariosByEstado_circuitAbierto_retornaListaVacia() {
        UUID estadoId = UUID.randomUUID();

        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, List<UserDto>> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("service unavailable"));
                });

        List<UserDto> result = usersClient.getUsuariosByEstado(estadoId);

        assertTrue(result.isEmpty());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void getUsuariosByEstado_responseBodyNull_retornaListaVacia() {
        UUID estadoId = UUID.randomUUID();

        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Supplier<List<UserDto>> supplier = inv.getArgument(0);
                    return supplier.get();
                });
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<UserDto> result = usersClient.getUsuariosByEstado(estadoId);

        assertTrue(result.isEmpty());
    }
}
