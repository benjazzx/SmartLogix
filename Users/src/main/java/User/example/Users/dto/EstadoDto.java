package User.example.Users.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

// DTO para deserializar respuestas HTTP del microservicio Estado
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDto {
    private UUID id;
    private String nombre;
    private String descripcion;
}
