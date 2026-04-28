package Inventario.example.Inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstanteRequestDTO {

    @NotBlank(message = "El código del estante es obligatorio")
    @Size(max = 20)
    private String codigo;

    @Size(max = 150)
    private String descripcion;

    @Positive(message = "El número de niveles debe ser positivo")
    private Integer numNiveles;

    @Positive(message = "La capacidad por nivel debe ser positiva")
    private Double capacidadPorNivel;

    private Boolean activo;
}
