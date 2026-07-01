package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Data
@Entity
@Table(name = "comuna")
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comuna perteneciente a una región")
public class ComunaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Nombre de la comuna", example = "Providencia")
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = false)
    @Schema(description = "Región a la que pertenece la comuna")
    private RegionModel region;
}
