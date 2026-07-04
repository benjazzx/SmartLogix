package Orden.example.Orden.repository;
import Orden.example.Orden.model.OrdenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrdenRepository extends JpaRepository<OrdenModel, Long> {

    @Query("SELECT DISTINCT o FROM OrdenModel o LEFT JOIN FETCH o.detalles")
    List<OrdenModel> findAllWithDetails();

    @Query("SELECT DISTINCT o FROM OrdenModel o LEFT JOIN FETCH o.detalles WHERE o.userId = :userId")
    List<OrdenModel> findByUserIdWithDetails(@Param("userId") UUID userId);
}
