package Estado.example.Estado.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoAssignedEvent {
    private String email;
    private UUID estadoId;
    private String estadoNombre;
    private String tipoEstadoNombre;
}
