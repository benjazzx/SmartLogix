package Inventario.example.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Mensaje publicado por Orden en RabbitMQ al crear un pedido (EP3 DSY1107). Inventario
// lo consume para asignar que bodega lo despacha — decision basada en ocupacion real,
// que Orden no tiene forma de calcular por si sola.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCreadaRabbitEvent implements Serializable {
    private Long ordenId;
    private UUID userId;
}
