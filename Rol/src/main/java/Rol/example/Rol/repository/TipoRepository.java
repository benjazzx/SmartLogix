package Rol.example.Rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import Rol.example.Rol.model.TipoModel;

@Repository
public interface TipoRepository extends JpaRepository<TipoModel, UUID> {
    Optional<TipoModel> findByNombre(String nombre);
}
