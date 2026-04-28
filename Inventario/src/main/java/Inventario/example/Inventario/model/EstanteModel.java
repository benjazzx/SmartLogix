package Inventario.example.Inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstanteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estante")
    private Long idEstante;

    @NotBlank(message = "El código del estante es obligatorio")
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    @Column(name = "codigo", nullable = false, length = 20, unique = true)
    private String codigo;

    @Size(max = 150, message = "La descripción no puede superar 150 caracteres")
    @Column(name = "descripcion", length = 150)
    private String descripcion;

    @Positive(message = "El número de niveles debe ser positivo")
    @Column(name = "num_niveles")
    private Integer numNiveles;

    @Positive(message = "La capacidad por nivel debe ser positiva")
    @Column(name = "capacidad_por_nivel")
    private Double capacidadPorNivel;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "estante", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EstPasiModel> pasillos = new ArrayList<>();

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
