package User.example.Users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Evento RabbitMQ (routing key usuario.registrado): tarea asíncrona de onboarding.
// Lo consume Configuracion para dejar listo el espacio de preferencias del usuario nuevo.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRegistradoRabbitEvent implements Serializable {
    private UUID userId;
    private String rolNombre;
}
