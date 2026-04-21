package Estado.example.Estado.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Entity
@Table(name = "estado")
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entidad que representa un estado asignable a un usuario en SmartLogix")
public class Estado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Identificador único del estado", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    @Schema(description = "Nombre del estado", example = "activo")
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 200)
    @Schema(description = "Descripción del estado", example = "Usuario activo en el sistema")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "tipo_de_estado_id", nullable = false)
    @Schema(description = "Tipo de estado al que pertenece")
    private TipoDeEstadoModel tipoDeEstado;
}
