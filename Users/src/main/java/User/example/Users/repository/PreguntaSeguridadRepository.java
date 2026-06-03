package User.example.Users.repository;

import User.example.Users.model.PreguntaSeguridadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PreguntaSeguridadRepository extends JpaRepository<PreguntaSeguridadModel, Long> {
    List<PreguntaSeguridadModel> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
