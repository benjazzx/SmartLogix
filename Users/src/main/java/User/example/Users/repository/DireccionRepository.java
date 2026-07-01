package User.example.Users.repository;

import User.example.Users.model.DireccionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DireccionRepository extends JpaRepository<DireccionModel, UUID> {
}
