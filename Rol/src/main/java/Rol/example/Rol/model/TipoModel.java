package Rol.example.Rol.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "tipo")
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Categoría que agrupa privilegios del sistema")
public class TipoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    @Schema(description = "Nombre del tipo", example = "LECTURA")
    private String nombre;
}
