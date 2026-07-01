package Producto.example.Producto.repository;

import Producto.example.Producto.model.ProductoModel;
import org.springframework.data.jpa.repository.JpaRepository;
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
    List<ProductoModel> findByCreadoPorId(UUID userId);
    List<ProductoModel> findByModificadoPorId(UUID userId);
    List<ProductoModel> findByIdBodega(Long idBodega);
    List<ProductoModel> findByIdBodegaAndIdPasillo(Long idBodega, Long idPasillo);
    List<ProductoModel> findByIdBodegaAndIdPasilloAndIdEstante(Long idBodega, Long idPasillo, Long idEstante);
}
