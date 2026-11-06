package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Mensaje publicado por Producto en RabbitMQ al crear un producto nuevo (EP3 DSY1107).
// Inventario lo consume para asignarle ubicacion (bodega/pasillo/estante) de forma
// asincrona, en vez de que el formulario de creacion la decida a mano.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCreadoRabbitEvent implements Serializable {
    private UUID productoId;
    private String nombre;
}
