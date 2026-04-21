package Rol.example.Rol.messaging;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import Rol.example.Rol.dto.RoleAssignedEvent;

@Component
public class RoleEventProcessor {

    @Autowired
    private StreamBridge streamBridge;

    public void publishRoleAssigned(String email, UUID roleId, String roleName) {
        RoleAssignedEvent event = new RoleAssignedEvent(email, roleId, roleName);
        streamBridge.send("role-assigned-topic", event);
    }
}
