package User.example.Users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Petición del microservicio administrador de RabbitMQ (EP3 DSY1107) para crear un exchange.
@Data
public class ExchangeRequestDto {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String nombre;

    // topic | direct | fanout
    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(topic|direct|fanout)$", message = "El tipo debe ser topic, direct o fanout")
    private String tipo = "topic";

    private boolean durable = true;
}
