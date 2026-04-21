package Estado.example.Estado.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Estado.example.Estado.Model.TipoDeEstadoModel;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoDeEstadoRepository extends JpaRepository<TipoDeEstadoModel, UUID> {
    Optional<TipoDeEstadoModel> findByNombre(String nombre);
}
