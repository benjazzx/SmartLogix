package Rol.example.Rol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO del evento que produce el microservicio Users cuando un nuevo usuario se registra.
// Si Users publicara un evento de registro, Rol podría consumirlo y asignar rol automáticamente.
// Por ahora la asignación se hace vía HTTP (GET /api/roles/assign?email=...) desde Users.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String email;
}
