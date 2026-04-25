package Inventario.example.Inventario.repository;

import Inventario.example.Inventario.model.EstanteModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstanteRepository extends JpaRepository<EstanteModel, Long> {

    Optional<EstanteModel> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<EstanteModel> findByActivoTrue();

    @Query("""
            SELECT DISTINCT e FROM EstanteModel e
            JOIN e.pasillos ep
            JOIN ep.pasillo p
            WHERE p.idPasillo = :idPasillo
            """)
    List<EstanteModel> findByPasilloId(Long idPasillo);

    @Query("""
            SELECT DISTINCT e FROM EstanteModel e
            JOIN e.pasillos ep
            JOIN ep.pasillo p
            JOIN p.bodega b
            WHERE b.idBodega = :idBodega
            """)
    List<EstanteModel> findByBodegaId(Long idBodega);
}
