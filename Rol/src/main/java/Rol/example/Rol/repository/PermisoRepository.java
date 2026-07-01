package Rol.example.Rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import Rol.example.Rol.model.PermisoModel;

@Repository
public interface PermisoRepository extends JpaRepository<PermisoModel, UUID> {
    List<PermisoModel> findByRolId(UUID rolId);
    List<PermisoModel> findByPrivilegioId(UUID privilegioId);
    boolean existsByRolIdAndPrivilegioId(UUID rolId, UUID privilegioId);
    void deleteByRolIdAndPrivilegioId(UUID rolId, UUID privilegioId);
}
