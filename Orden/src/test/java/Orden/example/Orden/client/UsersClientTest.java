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
@SuppressWarnings({"unchecked", "java:S100"})
class UsersClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private UsersClient usersClient;

    private static final String USERS_URL = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usersClient, "usersUrl", USERS_URL);
        when(circuitBreakerFactory.create("usersClient")).thenReturn(circuitBreaker);
    }

    @Test
    void getNombreUsuario_usuarioEncontradoConNombre_retornaNombre() {
        UUID userId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(USERS_URL + "/api/users/" + userId, Map.class))
                .thenReturn(Map.of("nombre", "Juan García", "correo", "juan@test.com"));

        String result = usersClient.getNombreUsuario(userId);

        assertEquals("Juan García", result);
    }

    @Test
    void getNombreUsuario_usuarioSinNombre_retornaNull() {
        UUID userId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(USERS_URL + "/api/users/" + userId, Map.class))
                .thenReturn(Map.of("correo", "juan@test.com"));

        String result = usersClient.getNombreUsuario(userId);

        assertNull(result);
    }

    @Test
    void getNombreUsuario_usuarioNull_retornaNull() {
        UUID userId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(restTemplate.getForObject(USERS_URL + "/api/users/" + userId, Map.class))
                .thenReturn(null);

        String result = usersClient.getNombreUsuario(userId);

        assertNull(result);
    }

    @Test
    void getNombreUsuario_circuitBreakerActiva_retornaNull() {
        UUID userId = UUID.randomUUID();
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(inv -> {
                    Function<Throwable, String> fallback = inv.getArgument(1);
                    return fallback.apply(new RuntimeException("servicio caído"));
                });

        String result = usersClient.getNombreUsuario(userId);

        assertNull(result);
    }
}
