package Orden.example.Orden.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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

    @Column(name = "producto_id")
    private UUID productoId;

    @Column(name = "producto_nombre", length = 200)
    private String productoNombre;

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private Integer cantidad;
}
