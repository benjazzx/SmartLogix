package User.example.Users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExchangeRequestDto {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(max = 255, message = "El nombre del exchange no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String nombre;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(topic|direct|fanout)$", message = "El tipo debe ser topic, direct o fanout")
    private String tipo = "topic";

    private boolean durable = true;
}
