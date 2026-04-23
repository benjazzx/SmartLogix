package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Data
@Entity
@Table(name = "usuario")
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Usuario del sistema SmartLogix")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "ID único del usuario")
    private UUID id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Column(nullable = false, length = 100)
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String apellido;

    @Column(nullable = false, unique = true, length = 12)
    @Schema(description = "RUT del usuario", example = "12345678-9")
    private String rut;

    @Column(nullable = false, unique = true, length = 150)
    @Schema(description = "Correo electrónico", example = "juan@smartlogix.cl")
    private String correo;

    @Column(nullable = false, length = 255)
    @Schema(description = "Contraseña del usuario (encriptada)")
    private String clave;

    @Column(length = 100)
    @Schema(description = "Cargo del usuario (bodeguero, transportista, admin — null para clientes)", example = "Bodeguero")
    private String cargo;

    @Column(nullable = false)
    @Schema(description = "Estado activo del usuario")
    private Boolean activo = true;

    // FK hacia el microservicio Rol (sin @ManyToOne — es otro servicio)
    @Column(name = "rol_id")
    @Schema(description = "UUID del rol asignado (referencia al microservicio Rol)")
    private UUID rolId;

    @Column(name = "rol_nombre", length = 50)
    @Schema(description = "Nombre del rol (desnormalizado para evitar consultas al servicio Rol)", example = "bodeguero")
    private String rolNombre;

    // FK hacia el microservicio Estado (sin @ManyToOne — es otro servicio)
    @Column(name = "estado_id")
    @Schema(description = "UUID del estado asignado (referencia al microservicio Estado)")
    private UUID estadoId;

    @Column(name = "estado_nombre", length = 50)
    @Schema(description = "Nombre del estado (desnormalizado para evitar consultas al servicio Estado)", example = "activo")
    private String estadoNombre;

    // Relación con Dirección (dentro del mismo microservicio)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_id")
    @Schema(description = "Dirección del usuario (principalmente para clientes)")
    private DireccionModel direccion;
}
