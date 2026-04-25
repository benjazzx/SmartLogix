package Orden.example.Orden.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "orden")
@NoArgsConstructor
@AllArgsConstructor
public class OrdenModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaOrden = LocalDateTime.now();

    // FK al microservicio Users — direccion del cliente
    @Column(name = "direccion_id", nullable = false)
    private Long direccionId;

    // FK al microservicio Users — solo clientes crean ordenes
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Desnormalizado para evitar llamadas HTTP en cada consulta
    @Column(name = "user_nombre", length = 200)
    private String userNombre;

    // Estado actual desnormalizado desde el ultimo historial
    @Column(name = "estado_actual", length = 100)
    private String estadoActual = "pendiente";

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<HistorialModel> historial = new ArrayList<>();

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DetalleOrdenModel> detalles = new ArrayList<>();
}
