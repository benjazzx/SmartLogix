package Rol.example.Rol.messaging;

import Rol.example.Rol.dto.RoleAssignedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleEventProcessorTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private RoleEventProcessor roleEventProcessor;

    @Test
    void publishRoleAssigned_enviaEventoAlTopicoCorrecto() {
        String email = "usuario@smartlogix.cl";
        UUID roleId = UUID.randomUUID();
        String roleName = "admin";

        roleEventProcessor.publishRoleAssigned(email, roleId, roleName);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq("role-assigned-topic"), payloadCaptor.capture());

        RoleAssignedEvent evento = (RoleAssignedEvent) payloadCaptor.getValue();
        assertEquals(email, evento.getEmail());
        assertEquals(roleId, evento.getRoleId());
        assertEquals(roleName, evento.getRoleName());
    }

    @Test
    void publishRoleAssigned_distintoRol_enviaEventoCorrecto() {
        String email = "bodeguero@smartlogix.cl";
        UUID roleId = UUID.randomUUID();
        String roleName = "bodeguero";

        roleEventProcessor.publishRoleAssigned(email, roleId, roleName);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq("role-assigned-topic"), captor.capture());

        RoleAssignedEvent evento = (RoleAssignedEvent) captor.getValue();
        assertEquals("bodeguero", evento.getRoleName());
    }

    @Test
    void publishRoleAssigned_llamaStreamBridgeExactamenteUnaVez() {
        roleEventProcessor.publishRoleAssigned("a@a.cl", UUID.randomUUID(), "cliente");

        verify(streamBridge, times(1)).send(anyString(), any());
    }
}
