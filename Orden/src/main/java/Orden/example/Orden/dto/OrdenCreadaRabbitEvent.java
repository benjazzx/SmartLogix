package Orden.example.Orden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

// Mensaje publicado en RabbitMQ al crear una orden (EP3 DSY1107). Inventario lo
// consume para asignar que bodega la despacha, segun la ocupacion real.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCreadaRabbitEvent implements Serializable {
    private Long ordenId;
    private UUID userId;
}
