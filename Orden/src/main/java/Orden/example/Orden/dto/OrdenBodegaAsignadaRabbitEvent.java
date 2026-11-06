package Orden.example.Orden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Evento que publica Inventario (routing key orden.bodega-asignada): bodega elegida para despachar.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenBodegaAsignadaRabbitEvent implements Serializable {
    private Long ordenId;
    private Long idBodega;
    private String nombreBodega;
}
