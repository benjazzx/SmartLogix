package Estado.example.Estado.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Estado.example.Estado.Model.Estado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, UUID> {
    Optional<Estado> findByNombre(String nombre);
    List<Estado> findByTipoDeEstadoNombre(String tipoNombre);
}
