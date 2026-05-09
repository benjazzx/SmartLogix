package Rol.example.Rol.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PrivilegioRequestDto {
    private String nombre;
    private String descripcion;
    private UUID tipoId;
}
