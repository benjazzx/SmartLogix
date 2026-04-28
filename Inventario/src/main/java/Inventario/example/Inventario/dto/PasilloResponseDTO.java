package Inventario.example.Inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasilloResponseDTO {

    private Long idPasillo;
    private String codigo;
    private String descripcion;
    private Integer numeroOrden;
    private Boolean activo;
    private Long idBodega;
    private String nombreBodega;
    private Integer totalEstantes;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
