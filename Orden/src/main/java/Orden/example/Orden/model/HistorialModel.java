package Orden.example.Orden.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "historial_orden")
@NoArgsConstructor
@AllArgsConstructor
public class HistorialModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "estado_id", nullable = false)
    private UUID estadoId;

    @Column(name = "estado_nombre", nullable = false, length = 100)
    private String estadoNombre;

    @Column(length = 500)
    private String comentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrdenModel orden;
}
