package Rol.example.Rol.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(
    name = "permiso",
    uniqueConstraints = @UniqueConstraint(columnNames = {"rol_id", "privilegio_id"})
)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Asignación de un privilegio a un rol — define qué puede hacer cada rol")
public class PermisoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK hacia Rol
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    @Schema(description = "Rol al que se le asigna el privilegio")
    private RolModel rol;

    // FK hacia Privilegio
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "privilegio_id", nullable = false)
    @Schema(description = "Privilegio asignado al rol")
    private PrivilegioModel privilegio;
}
