package Rol.example.Rol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "rol")
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Entidad que representa un rol de usuario en SmartLogix")
public class RolModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Identificador único del rol", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    @Schema(description = "Nombre del rol", example = "bodeguero")
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 200)
    @Schema(description = "Descripción del rol", example = "Encargado de gestionar el inventario del almacén")
    private String descripcion;
}
