package Producto.example.Producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoActualizadoEvent {

    public enum TipoEvento { CREADO, ACTUALIZADO, STOCK_CAMBIADO, DESACTIVADO }

    private UUID productoId;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private String categoriaNombre;
    private String estadoNombre;
    private Boolean activo;
    private TipoEvento tipoEvento;
}
