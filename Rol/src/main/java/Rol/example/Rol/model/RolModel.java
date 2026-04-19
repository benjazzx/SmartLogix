package Rol.example.Rol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Entity
@Table(name = "rol")
@AllArgsConstructor
@NoArgsConstructor
public class RolModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;
    
    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;
}
