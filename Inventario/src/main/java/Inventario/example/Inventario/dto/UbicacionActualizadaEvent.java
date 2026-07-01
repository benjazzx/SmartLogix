package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se crea o modifica una ubicación de estante en un pasillo.
 * Consumido por microservicios interesados en cambios de inventario físico (Orden, Envío).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionActualizadaEvent {

    private Long idEstPasi;

    private Long idEstante;
    private String codigoEstante;

    private Long idPasillo;
    private String codigoPasillo;

    private Long idBodega;
    private String nombreBodega;

    private Double ocupacionPct;
    private Boolean habilitada;

    /** CREADA | ACTUALIZADA | ELIMINADA */
    private String tipoEvento;

    private LocalDateTime timestamp;
}
