package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitud_recuperacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudRecuperacionModel {

    public enum ResultadoVerificacion { PENDIENTE, EXITOSO, BLOQUEADO, UTILIZADA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_verificacion", nullable = false, length = 20)
    @Builder.Default
    private ResultadoVerificacion resultadoVerificacion = ResultadoVerificacion.PENDIENTE;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}
