package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Resultado de la asignación de bodega de despacho (routing key orden.bodega-asignada).
// Lo consume Orden para guardar desde qué bodega se despachará el pedido.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenBodegaAsignadaRabbitEvent implements Serializable {
    private Long ordenId;
    private Long idBodega;
    private String nombreBodega;
}
