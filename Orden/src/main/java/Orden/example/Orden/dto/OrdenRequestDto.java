package Orden.example.Orden.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrdenRequestDto {

    @NotNull
    private UUID direccionId;

    // Nombre del cliente desnormalizado (evita llamadas HTTP en consultas)
    private String userNombre;

    @NotNull
    @Size(min = 1)
    private List<DetalleDto> detalles;

    @Data
    public static class DetalleDto {
        @NotNull
        private UUID productoId;

        @NotNull
        private Integer cantidad;
    }
}
