package Inventario.example.Inventario.repository;

import Inventario.example.Inventario.model.PasilloModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasilloRepository extends JpaRepository<PasilloModel, Long> {

    List<PasilloModel> findByBodega_IdBodega(Long idBodega);

    List<PasilloModel> findByBodega_IdBodegaAndActivoTrue(Long idBodega);

    boolean existsByCodigoIgnoreCaseAndBodega_IdBodega(String codigo, Long idBodega);

    @Query("SELECT p FROM PasilloModel p LEFT JOIN FETCH p.estantes ep LEFT JOIN FETCH ep.estante WHERE p.idPasillo = :id")
    Optional<PasilloModel> findByIdWithEstantes(Long id);

    @Query("SELECT p FROM PasilloModel p JOIN p.bodega b WHERE b.idBodega = :idBodega ORDER BY p.numeroOrden ASC")
    List<PasilloModel> findByBodegaOrdenados(Long idBodega);
}
