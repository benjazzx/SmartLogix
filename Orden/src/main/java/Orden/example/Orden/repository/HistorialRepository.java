package Orden.example.Orden.repository;

import Orden.example.Orden.model.HistorialModel;
import Orden.example.Orden.model.OrdenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialRepository extends JpaRepository<HistorialModel, Long> {
    List<HistorialModel> findByOrden(OrdenModel orden);
}
