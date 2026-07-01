package User.example.Users.client;

import User.example.Users.dto.RolDto;
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
class RolClientTest {

    @Mock private RestTemplate restTemplate;
    @SuppressWarnings("rawtypes")
    @Mock private CircuitBreakerFactory circuitBreakerFactory;
    @Mock private CircuitBreaker circuitBreaker;

    private RolClient rolClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rolClient = new RolClient();
        ReflectionTestUtils.setField(rolClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(rolClient, "circuitBreakerFactory", circuitBreakerFactory);
        ReflectionTestUtils.setField(rolClient, "rolUrl", "http://rol-service:8083");
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolById_respuestaOk_retornaRol() {
        UUID rolId = UUID.randomUUID();
        RolDto dto = new RolDto(rolId, "admin", "Administrador");

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(RolDto.class))).thenReturn(dto);

        RolDto result = rolClient.getRolById(rolId);

        assertNotNull(result);
        assertEquals("admin", result.getNombre());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolById_circuitAbierto_lanzaExcepcion() {
        UUID rolId = UUID.randomUUID();

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(1, java.util.function.Function.class)
                        .apply(new RuntimeException("service down")));

        assertThrows(RuntimeException.class, () -> rolClient.getRolById(rolId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolByNombre_encontrado_retornaRol() {
        RolDto cliente = new RolDto(UUID.randomUUID(), "cliente", "Cliente");
        RolDto admin = new RolDto(UUID.randomUUID(), "admin", "Admin");
        RolDto[] roles = {cliente, admin};

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(RolDto[].class))).thenReturn(roles);

        RolDto result = rolClient.getRolByNombre("cliente");

        assertNotNull(result);
        assertEquals("cliente", result.getNombre());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolByNombre_rolesNull_retornaNull() {
        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(RolDto[].class))).thenReturn(null);

        RolDto result = rolClient.getRolByNombre("cliente");

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolByNombre_noCoincide_retornaNull() {
        RolDto admin = new RolDto(UUID.randomUUID(), "admin", "Admin");
        RolDto[] roles = {admin};

        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(0, java.util.function.Supplier.class).get());
        when(restTemplate.getForObject(anyString(), eq(RolDto[].class))).thenReturn(roles);

        RolDto result = rolClient.getRolByNombre("cliente");

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getRolByNombre_circuitAbierto_retornaNull() {
        when(circuitBreaker.run(any(), any())).thenAnswer(inv ->
                inv.getArgument(1, java.util.function.Function.class)
                        .apply(new RuntimeException("service down")));

        RolDto result = rolClient.getRolByNombre("cliente");

        assertNull(result);
    }
}
