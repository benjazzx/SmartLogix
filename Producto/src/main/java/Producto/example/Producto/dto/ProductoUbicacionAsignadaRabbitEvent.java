package Producto.example.Producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Evento que publica Inventario (routing key producto.ubicacion-asignada): dónde quedó el producto.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUbicacionAsignadaRabbitEvent implements Serializable {
    private UUID productoId;
    private Long idBodega;
    private Long idPasillo;
    private Long idEstante;
}
