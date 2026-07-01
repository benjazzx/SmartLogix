package Inventario.example.Inventario.repository;

import Inventario.example.Inventario.model.AlertaBodegaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaBodegaRepository extends JpaRepository<AlertaBodegaModel, Long> {

    List<AlertaBodegaModel> findByLeidaFalseOrderByFechaDesc();

    boolean existsByIdEstanteAndLeidaFalse(Long idEstante);
}
