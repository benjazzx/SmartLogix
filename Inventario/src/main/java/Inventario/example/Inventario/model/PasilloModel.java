package Inventario.example.Inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pasillo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasilloModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pasillo")
    private Long idPasillo;

    @NotBlank(message = "El código del pasillo es obligatorio")
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Column(name = "numero_orden")
    private Integer numeroOrden;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @NotNull(message = "La bodega es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bodega", nullable = false)
    private BodegaModel bodega;

    @OneToMany(mappedBy = "pasillo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EstPasiModel> estantes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
