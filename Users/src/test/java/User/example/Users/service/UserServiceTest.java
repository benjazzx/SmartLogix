package User.example.Users.service;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.messaging.UserEventProducer;
import User.example.Users.model.UserModel;
import User.example.Users.repository.DireccionRepository;
import User.example.Users.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private UserEventProducer eventProducer;
    @Mock private CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    @Mock private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        // El circuit breaker ejecuta directamente la lógica (sin abrir el circuito)
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    @Test
    void getAllUsers_debeRetornarListaCompleta() {
        UserModel user = new UserModel();
        user.setNombre("Juan");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserModel> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getNombre());
    }

    @Test
    void getUserById_existente_debeRetornarUsuario() {
        UUID id = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserModel result = userService.getUserById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void getUserById_noExistente_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserById(id));
    }

    @Test
    void createUser_correoExistente_debeLanzarExcepcion() {
        UserRequestDto dto = new UserRequestDto();
        dto.setCorreo("duplicado@test.cl");
        dto.setRut("11111111-1");

        when(userRepository.existsByCorreo("duplicado@test.cl")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_valido_debeGuardarYPublicarEvento() {
        UserRequestDto dto = new UserRequestDto();
        dto.setNombre("Ana");
        dto.setApellido("Torres");
        dto.setRut("44444444-4");
        dto.setCorreo("ana@test.cl");
        dto.setClave("pass");
        dto.setRolNombre("cliente");

        when(userRepository.existsByCorreo(dto.getCorreo())).thenReturn(false);
        when(userRepository.existsByRut(dto.getRut())).thenReturn(false);

        UserModel saved = new UserModel();
        saved.setId(UUID.randomUUID());
        saved.setCorreo(dto.getCorreo());
        saved.setRolNombre("cliente");
        when(userRepository.save(any())).thenReturn(saved);

        doNothing().when(eventProducer).publishUserCreated(any());

        UserModel result = userService.createUser(dto);

        assertNotNull(result.getId());
        verify(eventProducer, times(1)).publishUserCreated(any());
    }

    @Test
    void deleteUser_noExistente_debeLanzarExcepcion() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.deleteUser(id));
    }

    @Test
    void actualizarRolPorCorreo_debeActualizarRol() {
        String correo = "test@test.cl";
        UUID rolId = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setCorreo(correo);

        when(userRepository.findByCorreo(correo)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.actualizarRolPorCorreo(correo, rolId, "bodeguero");

        assertEquals(rolId, user.getRolId());
        assertEquals("bodeguero", user.getRolNombre());
    }

    @Test
    void actualizarEstadoPorCorreo_estadoInactivo_debeDesactivarUsuario() {
        String correo = "test@test.cl";
        UUID estadoId = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setActivo(true);

        when(userRepository.findByCorreo(correo)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.actualizarEstadoPorCorreo(correo, estadoId, "inactivo");

        assertFalse(user.getActivo());
        assertEquals("inactivo", user.getEstadoNombre());
    }
}
