package User.example.Users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

// Evento publicado por el microservicio Estado en el tópico "estado-assigned-topic"
// Users lo consume para actualizar estadoId, estadoNombre y activo del usuario correspondiente
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoAssignedEvent {
    private String email;
    private UUID estadoId;
    private String estadoNombre;
    private String tipoEstadoNombre;
}
