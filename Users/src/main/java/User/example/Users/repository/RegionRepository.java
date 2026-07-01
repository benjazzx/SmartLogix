package User.example.Users.repository;

import User.example.Users.model.RegionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<RegionModel, UUID> {
    Optional<RegionModel> findByNombre(String nombre);
}
