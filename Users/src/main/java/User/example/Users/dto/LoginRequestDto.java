package User.example.Users.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String correo;
    private String clave;
}
