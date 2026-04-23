package Rol.example.Rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import Rol.example.Rol.model.PrivilegioModel;

@Repository
public interface PrivilegioRepository extends JpaRepository<PrivilegioModel, UUID> {
    Optional<PrivilegioModel> findByNombre(String nombre);
    List<PrivilegioModel> findByTipoId(UUID tipoId);
}
