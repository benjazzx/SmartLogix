package Rol.example.Rol.client;

import Rol.example.Rol.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersClientTest {

    @Mock private RestTemplate restTemplate;
    @SuppressWarnings("rawtypes")
    @Mock private CircuitBreakerFactory circuitBreakerFactory;
    @Mock private CircuitBreaker circuitBreaker;

    private UsersClient usersClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usersClient = new UsersClient();
        ReflectionTestUtils.setField(usersClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(usersClient, "circuitBreakerFactory", circuitBreakerFactory);
        ReflectionTestUtils.setField(usersClient, "usersUrl", "http://users-service:8082");
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsuariosByRol_respuestaOk_retornaLista() {
        UUID rolId = UUID.randomUUID();
        UserDto dto = mock(UserDto.class);
        List<UserDto> lista = List.of(dto);

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(lista));

        List<UserDto> result = usersClient.getUsuariosByRol(rolId);

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsuariosByRol_bodyNull_retornaListaVacia() {
        UUID rolId = UUID.randomUUID();

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<UserDto> result = usersClient.getUsuariosByRol(rolId);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsuariosByRol_circuitAbierto_retornaListaVacia() {
        UUID rolId = UUID.randomUUID();

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(1, java.util.function.Function.class)
                        .apply(new RuntimeException("service down")));

        List<UserDto> result = usersClient.getUsuariosByRol(rolId);

        assertTrue(result.isEmpty());
    }
}
