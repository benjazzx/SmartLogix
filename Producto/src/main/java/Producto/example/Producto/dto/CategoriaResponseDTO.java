package Producto.example.Producto.dto;

import Producto.example.Producto.model.CategoriaModel;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoriaResponseDTO {

    private UUID id;
    private String nombre;
    private String descripcion;

    public static CategoriaResponseDTO from(CategoriaModel c) {
        CategoriaResponseDTO dto = new CategoriaResponseDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setDescripcion(c.getDescripcion());
        return dto;
    }
}
