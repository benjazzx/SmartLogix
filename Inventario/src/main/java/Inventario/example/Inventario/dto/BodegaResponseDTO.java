package Inventario.example.Inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BodegaResponseDTO {

    private Long idBodega;
    private String nombre;
    private String direccion;
    private String ciudad;
    private String pais;
    private Double capacidadTotal;
    private Boolean activa;
    private Integer totalPasillos;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
