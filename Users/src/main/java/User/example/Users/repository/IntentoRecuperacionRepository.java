package User.example.Users.repository;

import User.example.Users.model.IntentoRecuperacionModel;
import User.example.Users.model.SolicitudRecuperacionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntentoRecuperacionRepository extends JpaRepository<IntentoRecuperacionModel, Long> {

    List<IntentoRecuperacionModel> findBySolicitudOrderByFechaDesc(SolicitudRecuperacionModel solicitud);
}
