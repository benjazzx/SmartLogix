package Producto.example.Producto.repository;

import Producto.example.Producto.model.HistorialStockModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialStockRepository extends JpaRepository<HistorialStockModel, Long> {

    List<HistorialStockModel> findByProductoIdOrderByFechaDesc(UUID productoId);
}
