package User.example.Users.dto;

import User.example.Users.model.DireccionModel;
import User.example.Users.model.UserModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponseDto {
    private UUID id;
    private String nombre;
    private String apellido;
    private String rut;
    private String correo;
    private String cargo;
    private Boolean activo;
    private UUID rolId;
    private String rolNombre;
    private UUID estadoId;
    private String estadoNombre;
    private DireccionModel direccion;

    public static UserResponseDto from(UserModel u) {
        return new UserResponseDto(
            u.getId(), u.getNombre(), u.getApellido(), u.getRut(), u.getCorreo(),
            u.getCargo(), u.getActivo(), u.getRolId(), u.getRolNombre(),
            u.getEstadoId(), u.getEstadoNombre(), u.getDireccion()
        );
    }
}
