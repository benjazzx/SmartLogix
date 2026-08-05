package User.example.Users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    @Size(min = 6, max = 100)
    private String clave;
}
