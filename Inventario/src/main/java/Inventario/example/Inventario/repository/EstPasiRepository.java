package Inventario.example.Inventario.repository;

import Inventario.example.Inventario.model.EstPasiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstPasiRepository extends JpaRepository<EstPasiModel, Long> {

    List<EstPasiModel> findByPasillo_IdPasillo(Long idPasillo);

    List<EstPasiModel> findByEstante_IdEstante(Long idEstante);

    Optional<EstPasiModel> findByEstante_IdEstanteAndPasillo_IdPasillo(Long idEstante, Long idPasillo);

    boolean existsByEstante_IdEstanteAndPasillo_IdPasillo(Long idEstante, Long idPasillo);

    List<EstPasiModel> findByHabilitadaTrue();

    @Query("""
            SELECT ep FROM EstPasiModel ep
            JOIN FETCH ep.estante e
            JOIN FETCH ep.pasillo p
            JOIN FETCH p.bodega b
            WHERE p.bodega.idBodega = :idBodega
            """)
    List<EstPasiModel> findByBodegaId(Long idBodega);

    @Query("""
            SELECT ep FROM EstPasiModel ep
            JOIN FETCH ep.estante
            JOIN FETCH ep.pasillo p
            WHERE p.idPasillo = :idPasillo
            ORDER BY ep.numeroFila ASC
            """)
    List<EstPasiModel> findByPasilloOrdenado(Long idPasillo);

    @Query("SELECT AVG(ep.ocupacionPct) FROM EstPasiModel ep WHERE ep.pasillo.bodega.idBodega = :idBodega")
    Double calcularOcupacionPromedioPorBodega(Long idBodega);
}
