package Estado.example.Estado.Dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EstadoRequestDto {
    private String nombre;
    private String descripcion;
    private UUID tipoDeEstadoId;
}
