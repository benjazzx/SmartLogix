package Configuracion.example.Configuracion.repository;

import Configuracion.example.Configuracion.model.PreferenciaModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreferenciaRepository extends JpaRepository<PreferenciaModel, Long> {

    List<PreferenciaModel> findAllByUserId(UUID userId);

    Optional<PreferenciaModel> findByUserIdAndClave(UUID userId, String clave);
}
