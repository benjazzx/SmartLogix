package Producto.example.Producto.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "producto")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaModel categoria;

    // Denormalizado — nombre del estado en el microservicio Estado (ej: "publicado", "sin_stock")
    @Column(name = "estado_nombre", length = 50)
    private String estadoNombre;

    // Imagen almacenada directamente en BD para que persista entre rebuilds del contenedor
    @Column(name = "imagen_data", columnDefinition = "bytea")
    private byte[] imagenData;

    @Column(name = "imagen_tipo", length = 100)
    private String imagenTipo;

    @Column(name = "id_bodega")
    private Long idBodega;

    @Column(name = "id_pasillo")
    private Long idPasillo;

    @Column(name = "id_estante")
    private Long idEstante;

    @Column(length = 100)
    @Builder.Default
    private String pais = "Chile";

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
