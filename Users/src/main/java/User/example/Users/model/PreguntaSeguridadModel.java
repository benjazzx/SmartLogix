package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "pregunta_seguridad")
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaSeguridadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pregunta", nullable = false, length = 200)
    private String pregunta;

    @Column(name = "respuesta", nullable = false, length = 255)
    private String respuesta;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}
