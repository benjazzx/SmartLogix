package Rol.example.Rol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Evento publicado por Users en "user-created-topic" cuando se registra un nuevo usuario.
// Rol lo consume y asigna el rol automáticamente según el dominio del email.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private java.util.UUID userId;
    private String email;
    private String rolNombre;
}
