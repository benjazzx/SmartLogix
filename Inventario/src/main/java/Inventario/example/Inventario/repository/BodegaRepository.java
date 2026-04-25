package Inventario.example.Inventario.repository;

import Inventario.example.Inventario.model.BodegaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BodegaRepository extends JpaRepository<BodegaModel, Long> {

    List<BodegaModel> findByActivaTrue();

    Optional<BodegaModel> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    @Query("SELECT b FROM BodegaModel b LEFT JOIN FETCH b.pasillos WHERE b.idBodega = :id")
    Optional<BodegaModel> findByIdWithPasillos(Long id);

    List<BodegaModel> findByCiudadIgnoreCase(String ciudad);

    List<BodegaModel> findByPaisIgnoreCase(String pais);
}
