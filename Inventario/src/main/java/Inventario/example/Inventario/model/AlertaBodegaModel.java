package Inventario.example.Inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta_bodega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaBodegaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_estante", nullable = false)
    private Long idEstante;

    @Column(name = "codigo_estante", length = 20)
    private String codigoEstante;

    @Column(name = "id_bodega")
    private Long idBodega;

    @Column(name = "ocupacion_pct", nullable = false)
    private Double ocupacionPct;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "leida", nullable = false)
    @Builder.Default
    private Boolean leida = false;
}
