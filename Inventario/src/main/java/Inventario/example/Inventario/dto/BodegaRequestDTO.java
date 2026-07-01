package Inventario.example.Inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodegaRequestDTO {

    @NotBlank(message = "El nombre de la bodega es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 255)
    private String direccion;

    @Size(max = 100)
    private String ciudad;

    @Size(max = 100)
    private String pais;

    private Double capacidadTotal;

    private Boolean activa;
}
