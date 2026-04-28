package Producto.example.Producto.dto;

import Producto.example.Producto.model.ProductoModel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductoResponseDTO {

    private UUID id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private UUID categoriaId;
    private String categoriaNombre;
    private String estadoNombre;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static ProductoResponseDTO from(ProductoModel p) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setPrecio(p.getPrecio());
        dto.setStock(p.getStock());
        dto.setCategoriaId(p.getCategoria().getId());
        dto.setCategoriaNombre(p.getCategoria().getNombre());
        dto.setEstadoNombre(p.getEstadoNombre());
        dto.setActivo(p.getActivo());
        dto.setFechaCreacion(p.getFechaCreacion());
        dto.setFechaActualizacion(p.getFechaActualizacion());
        return dto;
    }
}
