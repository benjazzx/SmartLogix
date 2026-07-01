package Inventario.example.Inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tabla intermedia entre EstanteModel y PasilloModel.
 * Almacena la ubicación de un estante dentro de un pasillo específico
 * junto con metadatos como posición y capacidad actual.
 */
@Entity
@Table(name = "est_pasi",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estante_pasillo",
                columnNames = {"id_estante", "id_pasillo"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstPasiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_est_pasi")
    private Long idEstPasi;

    @NotNull(message = "El estante es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estante", nullable = false)
    private EstanteModel estante;

    @NotNull(message = "El pasillo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pasillo", nullable = false)
    private PasilloModel pasillo;

    /**
     * Posición dentro del pasillo (e.g. lado izquierdo/derecho, numeración)
     */
    @Column(name = "posicion", length = 50)
    private String posicion;

    /**
     * Número de fila o secuencia dentro del pasillo
     */
    @Column(name = "numero_fila")
    private Integer numeroFila;

    /**
     * Capacidad de almacenamiento ocupada actualmente (porcentaje 0-100)
     */
    @Column(name = "ocupacion_pct")
    @Builder.Default
    private Double ocupacionPct = 0.0;

    /**
     * Indica si esta ubicación está habilitada para almacenar productos
     */
    @Column(name = "habilitada", nullable = false)
    @Builder.Default
    private Boolean habilitada = true;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
