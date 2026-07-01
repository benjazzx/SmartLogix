package Rol.example.Rol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

// DTO que representa el mensaje que viaja por Kafka.
// Cuando Rol asigna un rol a un usuario, serializa este objeto a JSON y lo envía al tópico.
// El microservicio Users lo deserializa y usa los datos para actualizar el campo id_rol del usuario.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignedEvent {
    private String email;   // identifica al usuario en el microservicio Users
    private UUID roleId;    // UUID del rol asignado
    private String roleName;
}
