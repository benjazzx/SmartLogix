package Rol.example.Rol.messaging;

import Rol.example.Rol.dto.UserRegisteredEvent;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    private static final String ROL_ADMIN = "admin";

    @Mock
    private RolService rolService;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    @Test
    void userRegisteredConsumer_asignaRolAlEmail() {
        UserRegisteredEvent evento = new UserRegisteredEvent(UUID.randomUUID(), "nuevo@smartlogix.cl", null);
        RolModel rolAsignado = new RolModel(UUID.randomUUID(), "cliente", "Usuario final");
        when(rolService.assignRoleFromEvent("nuevo@smartlogix.cl", null)).thenReturn(rolAsignado);

        Consumer<UserRegisteredEvent> consumer = userEventConsumer.userRegisteredConsumer();
        consumer.accept(evento);

        verify(rolService).assignRoleFromEvent("nuevo@smartlogix.cl", null);
    }

    @Test
    void userRegisteredConsumer_rolServiceRetornaNull_noLanzaExcepcion() {
        UserRegisteredEvent evento = new UserRegisteredEvent(UUID.randomUUID(), "sinrol@test.cl", null);
        when(rolService.assignRoleFromEvent("sinrol@test.cl", null)).thenReturn(null);

        Consumer<UserRegisteredEvent> consumer = userEventConsumer.userRegisteredConsumer();

        assertDoesNotThrow(() -> consumer.accept(evento));
        verify(rolService).assignRoleFromEvent("sinrol@test.cl", null);
    }

    @Test
    void userRegisteredConsumer_llamaRolServiceConEmailYRolNombre() {
        String email = "admin@empresa.cl";
        UserRegisteredEvent evento = new UserRegisteredEvent(UUID.randomUUID(), email, ROL_ADMIN);
        when(rolService.assignRoleFromEvent(email, ROL_ADMIN))
                .thenReturn(new RolModel(UUID.randomUUID(), ROL_ADMIN, "desc"));

        Consumer<UserRegisteredEvent> consumer = userEventConsumer.userRegisteredConsumer();
        consumer.accept(evento);

        verify(rolService, times(1)).assignRoleFromEvent(email, ROL_ADMIN);
    }
}
