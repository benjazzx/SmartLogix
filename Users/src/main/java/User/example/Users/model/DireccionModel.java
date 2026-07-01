package User.example.Users.model;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Data
@Entity
@Table(name = "direccion")
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dirección física asociada a un usuario o una orden")
public class DireccionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    @Schema(description = "Nombre de la calle", example = "Av. Providencia")
    private String calle;

    @Column(nullable = false, length = 20)
    @Schema(description = "Número del domicilio", example = "1234")
    private String numero;

    @Column(name = "codigo_postal", length = 10)
    @Schema(description = "Código postal", example = "7500000")
    private String codigoPostal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comuna_id", nullable = false)
    @Schema(description = "Comuna donde se ubica la dirección")
    private ComunaModel comuna;
}
