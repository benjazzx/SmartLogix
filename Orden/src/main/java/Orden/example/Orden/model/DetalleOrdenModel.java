package Orden.example.Orden.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Data
@Entity
@Table(name = "detalle_orden")
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrdenModel orden;

    // null por ahora hasta que exista microservicio Inventario
    @Column(name = "producto_id")
    private UUID productoId;

    @Column(nullable = false)
    private Integer cantidad;
}
