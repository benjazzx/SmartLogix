package User.example.Users.repository;

import User.example.Users.model.ComunaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComunaRepository extends JpaRepository<ComunaModel, UUID> {
    Optional<ComunaModel> findByNombre(String nombre);
}
