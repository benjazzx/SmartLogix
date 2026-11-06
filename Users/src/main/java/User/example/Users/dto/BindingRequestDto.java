package User.example.Users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Petición del microservicio administrador de RabbitMQ (EP3 DSY1107) para enlazar
// una cola existente a un exchange existente con una routing key.
@Data
public class BindingRequestDto {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String cola;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    @Pattern(regexp = "^[a-zA-Z0-9._*#-]+$", message = "La routing key solo puede tener letras, números, puntos, guiones, guiones bajos y los comodines * #")
    private String routingKey;
}
