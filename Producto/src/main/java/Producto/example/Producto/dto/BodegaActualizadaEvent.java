package Producto.example.Producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evento consumido cuando Inventario crea, actualiza o desactiva una bodega.
 * Si tipoEvento es DESACTIVADA, los productos con esa bodega asignada quedan sin ubicación.
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
