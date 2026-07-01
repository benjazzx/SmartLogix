package User.example.Users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String tipo = "Bearer";
    private UUID userId;
    private String correo;
    private String rolNombre;
}
