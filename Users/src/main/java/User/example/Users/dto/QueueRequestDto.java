package User.example.Users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Petición del microservicio administrador de RabbitMQ (EP3 DSY1107) para crear una cola.
@Data
public class QueueRequestDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String nombre;

    private boolean durable = true;
}
