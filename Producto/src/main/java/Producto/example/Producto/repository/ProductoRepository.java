package Producto.example.Producto.repository;

import Producto.example.Producto.model.ProductoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<ProductoModel, UUID> {

    // UPDATE atómico: solo descuenta si hay stock suficiente en el mismo statement,
    // así dos pedidos concurrentes no pueden pasar ambos contra el mismo stock leído
    // (a diferencia de un "leer stock, comparar en Java, luego guardar").
    @Modifying
    @Query("UPDATE ProductoModel p SET p.stock = p.stock - :cantidad " +
           "WHERE p.id = :id AND p.stock >= :cantidad")
    int reservarStockAtomico(@Param("id") UUID id, @Param("cantidad") int cantidad);

    List<ProductoModel> findByActivoTrue();
    List<ProductoModel> findByActivoTrueAndPais(String pais);
    List<ProductoModel> findByCategoria_Id(UUID categoriaId);
    List<ProductoModel> findByNombreContainingIgnoreCase(String nombre);
    List<ProductoModel> findByStockLessThanEqual(Integer umbral);
    List<ProductoModel> findByPaisIgnoreCase(String pais);
    List<ProductoModel> findByCreadoPorId(UUID userId);
    List<ProductoModel> findByModificadoPorId(UUID userId);
    List<ProductoModel> findByIdBodega(Long idBodega);
    List<ProductoModel> findByIdBodegaAndIdPasillo(Long idBodega, Long idPasillo);
    List<ProductoModel> findByIdBodegaAndIdPasilloAndIdEstante(Long idBodega, Long idPasillo, Long idEstante);
}
