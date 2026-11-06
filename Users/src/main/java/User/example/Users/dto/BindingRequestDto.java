package User.example.Users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BindingRequestDto {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = 255, message = "El nombre de la cola no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String cola;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = 255, message = "El nombre del exchange no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede tener letras, números, puntos, guiones y guiones bajos")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    @Size(max = 255, message = "La routing key no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._*#-]+$", message = "La routing key solo puede tener letras, números, puntos, guiones, guiones bajos y los comodines * #")
    private String routingKey;
}
