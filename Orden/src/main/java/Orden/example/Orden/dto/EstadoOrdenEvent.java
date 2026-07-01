package Orden.example.Orden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoOrdenEvent {

    private Long ordenId;
    private UUID userId;
    private UUID estadoId;
    private String estadoNombre;
    private String comentario;
    private LocalDateTime fecha;
}
