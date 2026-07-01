package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Data
@Entity
@Table(name = "region")
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Región geográfica de Chile")
public class RegionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    @Schema(description = "Nombre de la región", example = "Región Metropolitana")
    private String nombre;
}
