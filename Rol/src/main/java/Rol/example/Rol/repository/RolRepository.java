package Rol.example.Rol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

import Rol.example.Rol.model.RolModel;

// @Repository marca esta interfaz como capa de acceso a datos.
// Al extender JpaRepository, Spring genera automáticamente la implementación con:
// findAll(), findById(), save(), deleteById(), count(), etc.
// No necesitamos escribir SQL — Spring lo genera solo.
// Los parámetros de JpaRepository son: <TipoDeEntidad, TipoDeLaClavePrimaria>
@Repository
public interface RolRepository extends JpaRepository<RolModel, UUID> {

    // Spring interpreta el nombre del método y genera el SQL automáticamente:
    // SELECT * FROM rol WHERE nombre = ?
    // Optional<> evita NullPointerException: si no encuentra nada, retorna Optional.empty()
    Optional<RolModel> findByNombre(String nombre);
}
