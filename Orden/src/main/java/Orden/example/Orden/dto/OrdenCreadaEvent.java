package Orden.example.Orden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCreadaEvent {

    private Long ordenId;
    private UUID userId;
    private String userNombre;
    private UUID direccionId;
    private LocalDateTime fechaOrden;
    private List<DetalleDto> detalles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleDto {
        private UUID productoId;
        private Integer cantidad;
        private String productoNombre;
        private BigDecimal precioUnitario;
    }
}
