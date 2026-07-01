package User.example.Users.messaging;

import User.example.Users.dto.EstadoAssignedEvent;
import User.example.Users.dto.RoleAssignedEvent;
import User.example.Users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    @Test
    void roleAssignedConsumer_actualizaRolDelUsuario() {
        UUID roleId = UUID.randomUUID();
        RoleAssignedEvent evento = new RoleAssignedEvent("usuario@test.cl", roleId, "admin");

        Consumer<RoleAssignedEvent> consumer = userEventConsumer.roleAssignedConsumer();
        consumer.accept(evento);

        verify(userService).actualizarRolPorCorreo("usuario@test.cl", roleId, "admin");
    }

    @Test
    void roleAssignedConsumer_distintoRol_actualizaCorrectamente() {
        UUID roleId = UUID.randomUUID();
        RoleAssignedEvent evento = new RoleAssignedEvent("bodeguero@test.cl", roleId, "bodeguero");

        Consumer<RoleAssignedEvent> consumer = userEventConsumer.roleAssignedConsumer();
        assertDoesNotThrow(() -> consumer.accept(evento));

        verify(userService).actualizarRolPorCorreo("bodeguero@test.cl", roleId, "bodeguero");
    }

    @Test
    void estadoAssignedConsumer_actualizaEstadoDelUsuario() {
        UUID estadoId = UUID.randomUUID();
        EstadoAssignedEvent evento = new EstadoAssignedEvent("usuario@test.cl", estadoId, "activo", "cuenta");

        Consumer<EstadoAssignedEvent> consumer = userEventConsumer.estadoAssignedConsumer();
        consumer.accept(evento);

        verify(userService).actualizarEstadoPorCorreo("usuario@test.cl", estadoId, "activo");
    }

    @Test
    void estadoAssignedConsumer_distintoEstado_actualizaCorrectamente() {
        UUID estadoId = UUID.randomUUID();
        EstadoAssignedEvent evento = new EstadoAssignedEvent("emp@test.cl", estadoId, "disponible", "laboral");

        Consumer<EstadoAssignedEvent> consumer = userEventConsumer.estadoAssignedConsumer();
        assertDoesNotThrow(() -> consumer.accept(evento));

        verify(userService).actualizarEstadoPorCorreo("emp@test.cl", estadoId, "disponible");
    }
}
