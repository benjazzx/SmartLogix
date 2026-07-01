package Inventario.example.Inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstanteResponseDTO {

    private Long idEstante;
    private String codigo;
    private String descripcion;
    private Integer numNiveles;
    private Double capacidadPorNivel;
    private Integer capacidadTotal;
    private Integer stockActual;
    private Double ocupacionPct;
    private Boolean activo;
    private Integer totalPasillosAsignados;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
