package Producto.example.Producto.repository;

import Producto.example.Producto.model.ProductoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<ProductoModel, UUID> {
    List<ProductoModel> findByActivoTrue();
    List<ProductoModel> findByCategoria_Id(UUID categoriaId);
    List<ProductoModel> findByNombreContainingIgnoreCase(String nombre);
    List<ProductoModel> findByStockLessThanEqual(Integer umbral);
}
