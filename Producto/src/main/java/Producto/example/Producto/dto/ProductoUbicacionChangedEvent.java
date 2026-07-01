package Producto.example.Producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUbicacionChangedEvent {
    private String eventId;
    private Long idEstante;
    private int delta;
}
