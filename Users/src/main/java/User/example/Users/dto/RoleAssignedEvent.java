package User.example.Users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

// Evento publicado por el microservicio Rol en el tópico "role-assigned-topic"
// Users lo consume para actualizar rolId y rolNombre del usuario correspondiente
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignedEvent {
    private String email;
    private UUID roleId;
    private String roleName;
}
