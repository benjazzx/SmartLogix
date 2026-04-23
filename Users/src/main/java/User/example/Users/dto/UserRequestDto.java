package User.example.Users.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserRequestDto {
    private String nombre;
    private String apellido;
    private String rut;
    private String correo;
    private String clave;
    private String cargo;
    private UUID direccionId;
    private UUID rolId;
    private String rolNombre;
}
