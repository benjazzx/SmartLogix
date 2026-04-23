package Estado.example.Estado.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

// DTO para deserializar respuestas HTTP del microservicio Users
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String nombre;
    private String apellido;
    private String correo;
    private String cargo;
    private Boolean activo;
    private UUID rolId;
    private String rolNombre;
    private UUID estadoId;
    private String estadoNombre;
}
