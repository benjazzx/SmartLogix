package Orden.example.Orden.repository;
import Orden.example.Orden.model.OrdenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrdenRepository extends JpaRepository<OrdenModel, Long> {
    List<OrdenModel> findByUserId(UUID userId);
}
