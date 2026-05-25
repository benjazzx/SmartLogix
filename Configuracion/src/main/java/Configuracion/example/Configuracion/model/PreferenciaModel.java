package Configuracion.example.Configuracion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "preferencia",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "clave"})
)
@Getter @Setter @NoArgsConstructor
public class PreferenciaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String clave;

    @Column(nullable = false, length = 500)
    private String valor;

    public PreferenciaModel(UUID userId, String clave, String valor) {
        this.userId = userId;
        this.clave  = clave;
        this.valor  = valor;
    }
}
