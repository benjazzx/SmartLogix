package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import Inventario.example.Inventario.exception.SinCapacidadException;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.EstPasiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Lógica de asignación logística que dispara RabbitMQ. Vive en un servicio aparte para que la
 * transacción se confirme (commit) ANTES de que el consumidor haga ACK del mensaje: si el commit
 * fallara, el mensaje no se habría confirmado y se reintenta o va a la DLQ, en vez de perderse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticaService {

    private final EstPasiRepository estPasiRepository;
    private final BodegaRepository bodegaRepository;

    @Transactional(rollbackFor = Exception.class)
    public ProductoUbicacionAsignadaRabbitEvent asignarUbicacion(UUID productoId, String nombreProducto) {
        EstPasiModel destino = elegirMejorUbicacion();
        double pctAntes = destino.getOcupacionPct() != null ? destino.getOcupacionPct() : 0.0;
        destino.setOcupacionPct(Math.min(100.0, pctAntes + 1.0));
        estPasiRepository.save(destino);

        // Se leen aquí, dentro de la transacción: estante/pasillo/bodega son relaciones lazy.
        Long idEstante = destino.getEstante().getIdEstante();
        Long idPasillo = destino.getPasillo().getIdPasillo();
        Long idBodega = destino.getPasillo().getBodega().getIdBodega();

        log.info("[Inventario RABBIT] Producto '{}' ({}) asignado — estante={} pasillo={} bodega='{}' ocupación {}%→{}%",
                nombreProducto, productoId,
                destino.getEstante().getCodigo(), idPasillo,
                destino.getPasillo().getBodega().getNombre(), pctAntes, destino.getOcupacionPct());

        return new ProductoUbicacionAsignadaRabbitEvent(productoId, idBodega, idPasillo, idEstante);
    }

    @Transactional(readOnly = true)
    public OrdenBodegaAsignadaRabbitEvent elegirBodegaDespacho(Long ordenId) {
        List<BodegaModel> activas = bodegaRepository.findByActivaTrue();
        if (activas.isEmpty()) {
            throw new SinCapacidadException("No hay bodegas activas");
        }
        BodegaModel bodega = activas.stream()
                .min(Comparator.comparing(b -> {
                    Double prom = estPasiRepository.calcularOcupacionPromedioPorBodega(b.getIdBodega());
                    return prom != null ? prom : 0.0;
                }))
                .orElseThrow(() -> new SinCapacidadException("No hay bodegas activas"));

        log.info("[Inventario RABBIT] Orden #{} asignada para despacho desde bodega '{}' (id={})",
                ordenId, bodega.getNombre(), bodega.getIdBodega());

        return new OrdenBodegaAsignadaRabbitEvent(ordenId, bodega.getIdBodega(), bodega.getNombre());
    }

    private EstPasiModel elegirMejorUbicacion() {
        return estPasiRepository.findByHabilitadaTrue().stream()
                .min(Comparator.comparing(ep -> ep.getOcupacionPct() != null ? ep.getOcupacionPct() : 0.0))
                .orElseThrow(() -> new SinCapacidadException("No hay ubicaciones habilitadas en ningún pasillo"));
    }
}
