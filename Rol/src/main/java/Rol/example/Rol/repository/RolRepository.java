package Rol.example.Rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

import Rol.example.Rol.model.RolModel;

@Repository
public interface RolRepository extends JpaRepository<RolModel, UUID> {
    Optional<RolModel> findByNombre(String nombre);
}
