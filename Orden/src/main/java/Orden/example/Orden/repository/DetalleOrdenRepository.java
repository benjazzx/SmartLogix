package Orden.example.Orden.repository;

import Orden.example.Orden.model.DetalleOrdenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleOrdenRepository extends JpaRepository<DetalleOrdenModel, Long> {
}
