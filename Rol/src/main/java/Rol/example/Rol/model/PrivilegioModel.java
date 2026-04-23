package Rol.example.Rol.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "privilegio")
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Acción específica que puede realizar un rol en el sistema")
public class PrivilegioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    @Schema(description = "Nombre del privilegio", example = "VER_INVENTARIO")
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 200)
    @Schema(description = "Descripción del privilegio", example = "Permite visualizar el stock de productos")
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_id", nullable = false)
    @Schema(description = "Tipo al que pertenece este privilegio")
    private TipoModel tipo;
}
