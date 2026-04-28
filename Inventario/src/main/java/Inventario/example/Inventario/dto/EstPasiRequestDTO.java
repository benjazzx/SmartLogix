package Inventario.example.Inventario.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstPasiRequestDTO {

    @NotNull(message = "El id del estante es obligatorio")
    private Long idEstante;

    @NotNull(message = "El id del pasillo es obligatorio")
    private Long idPasillo;

    @Size(max = 50)
    private String posicion;

    private Integer numeroFila;

    @DecimalMin(value = "0.0", message = "La ocupación debe ser >= 0")
    @DecimalMax(value = "100.0", message = "La ocupación debe ser <= 100")
    private Double ocupacionPct;

    private Boolean habilitada;

    @Size(max = 255)
    private String observaciones;
}
