package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Resultado de la asignación de ubicación (routing key producto.ubicacion-asignada).
// Lo consume Producto para guardar dónde quedó almacenado el producto.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUbicacionAsignadaRabbitEvent implements Serializable {
    private UUID productoId;
    private Long idBodega;
    private Long idPasillo;
    private Long idEstante;
}
