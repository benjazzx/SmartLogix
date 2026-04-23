package User.example.Users.repository;

import User.example.Users.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserModel, UUID> {
    Optional<UserModel> findByCorreo(String correo);
    Optional<UserModel> findByRut(String rut);
    List<UserModel> findByRolId(UUID rolId);
    List<UserModel> findByEstadoId(UUID estadoId);
    List<UserModel> findByActivo(Boolean activo);
    boolean existsByCorreo(String correo);
    boolean existsByRut(String rut);
}
