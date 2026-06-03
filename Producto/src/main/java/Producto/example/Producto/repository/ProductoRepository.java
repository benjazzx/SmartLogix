package Producto.example.Producto.repository;

import Producto.example.Producto.model.ProductoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<ProductoModel, UUID> {
    List<ProductoModel> findByActivoTrue();
    List<ProductoModel> findByActivoTrueAndPais(String pais);
    List<ProductoModel> findByCategoria_Id(UUID categoriaId);
    List<ProductoModel> findByNombreContainingIgnoreCase(String nombre);
    List<ProductoModel> findByStockLessThanEqual(Integer umbral);
    List<ProductoModel> findByPaisIgnoreCase(String pais);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM ProductoModel p WHERE p.idEstante = :idEstante AND p.activo = true")
    Integer sumStockByIdEstante(@Param("idEstante") Long idEstante);
}
