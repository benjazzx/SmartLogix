package User.example.Users.repository;

import User.example.Users.model.SolicitudRecuperacionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SolicitudRecuperacionRepository extends JpaRepository<SolicitudRecuperacionModel, Long> {
    List<SolicitudRecuperacionModel> findByEstadoOrderByFechaSolicitudDesc(String estado);
    List<SolicitudRecuperacionModel> findByUserIdOrderByFechaSolicitudDesc(UUID userId);
    List<SolicitudRecuperacionModel> findAllByOrderByFechaSolicitudDesc();
}
