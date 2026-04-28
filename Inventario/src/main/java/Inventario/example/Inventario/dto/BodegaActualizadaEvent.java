package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento publicado cuando se crea o modifica una bodega.
 * Permite que otros microservicios (Envío) conozcan las bodegas disponibles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodegaActualizadaEvent {

    private Long idBodega;
    private String nombre;
    private String ciudad;
    private String pais;
    private Boolean activa;

    /** CREADA | ACTUALIZADA | DESACTIVADA */
    private String tipoEvento;

    private LocalDateTime timestamp;
}
