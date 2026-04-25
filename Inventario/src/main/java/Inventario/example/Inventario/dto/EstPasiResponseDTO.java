package Inventario.example.Inventario.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstPasiResponseDTO {

    private Long idEstPasi;

    private Long idEstante;
    private String codigoEstante;
    private String descripcionEstante;
    private Integer numNiveles;

    private Long idPasillo;
    private String codigoPasillo;
    private String descripcionPasillo;

    private Long idBodega;
    private String nombreBodega;

    private String posicion;
    private Integer numeroFila;
    private Double ocupacionPct;
    private Boolean habilitada;
    private String observaciones;

    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaActualizacion;
}
