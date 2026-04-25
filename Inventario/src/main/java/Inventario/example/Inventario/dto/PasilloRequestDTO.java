package Inventario.example.Inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasilloRequestDTO {

    @NotBlank(message = "El código del pasillo es obligatorio")
    @Size(max = 20)
    private String codigo;

    @Size(max = 150)
    private String descripcion;

    private Integer numeroOrden;

    private Boolean activo;

    @NotNull(message = "El id de bodega es obligatorio")
    private Long idBodega;
}
