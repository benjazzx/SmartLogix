package User.example.Users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

// Evento publicado por Users en "user-created-topic" cuando se registra un nuevo usuario
// Otros microservicios pueden reaccionar a este evento (ej: Estado para asignar estado inicial)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private UUID userId;
    private String email;
    private String rolNombre;
}
