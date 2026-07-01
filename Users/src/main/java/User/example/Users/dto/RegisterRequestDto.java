package User.example.Users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequestDto {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String rut;

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    @Size(min = 6)
    private String clave;

    // Opcional en el registro — el cliente puede agregarla después
    private UUID direccionId;
}
