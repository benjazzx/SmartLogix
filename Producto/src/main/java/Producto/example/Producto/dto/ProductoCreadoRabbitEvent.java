package Producto.example.Producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCreadoRabbitEvent implements Serializable {
    private UUID productoId;
    private String nombre;
}
