package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "solicitud_recuperacion")
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRecuperacionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "nombre_usuario", length = 200)
    private String nombreUsuario;

    // PENDIENTE | APROBADA | RECHAZADA
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "clave_temporal", length = 255)
    private String claveTemporal;

    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @PrePersist
    protected void onCreate() {
        fechaSolicitud = LocalDateTime.now();
        estado = "PENDIENTE";
    }
}
