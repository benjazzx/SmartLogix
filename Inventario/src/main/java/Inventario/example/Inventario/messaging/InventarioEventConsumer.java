package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenCreadaEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionChangedEvent;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
import Inventario.example.Inventario.service.AlertaBodegaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioEventConsumer {

    private final EstanteRepository estanteRepository;
    private final EstPasiRepository estPasiRepository;
    private final AlertaBodegaService alertaBodegaService;

    @Value("${inventario.capacidad-default:1000}")
    private int capacidadDefault;

    @Value("${inventario.ocupacion-umbral-alerta:80.0}")
    private double umbralAlerta;

    // Idempotencia en memoria: descarta eventos duplicados (se reinicia con el servicio)
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
    private static final int MAX_PROCESSED_EVENTS = 10_000;

    @Bean
    public Consumer<OrdenCreadaEvent> onOrdenCreada() {
        return event -> {
            log.info("[Inventario] Orden recibida — ordenId={} userId={}", event.getOrdenId(), event.getUserId());
            if (event.getDetalles() != null) {
                event.getDetalles().forEach(detalle -> procesarDetalle(detalle, event));
            }
        };
    }

    // Orden ya reserva el stock de forma síncrona y atómica al crear el pedido (ver
    // OrdenService.createOrden / ProductoClient.reservarStock en el servicio Orden). Este
    // consumer NO debe volver a descontar — solo deja registro de que la orden llegó, para
    // trazabilidad. Descontar aquí también duplicaría el descuento de stock por cada pedido.
    private void procesarDetalle(OrdenCreadaEvent.DetalleDto detalle, OrdenCreadaEvent event) {
        try {
            if (detalle.getProductoId() == null || detalle.getCantidad() == null) return;
            log.info("[Inventario] Detalle de orden registrado (stock ya reservado por Orden) — productoId={} cantidad={} ordenId={}",
                    detalle.getProductoId(), detalle.getCantidad(), event.getOrdenId());
        } catch (Exception e) {
            log.error("[Inventario] Error procesando detalle productoId={}: {}", detalle.getProductoId(), e.getMessage());
        }
    }

    /**
     * Consume eventos de cambio de ubicación física de un producto en bodega.
     * Actualiza stockActual del estante y genera alerta si la ocupación supera el umbral.
     * Binding: onProductoUbicacionChanged-in-0 → producto-ubicacion-changed-topic
     */
    @Bean
    public Consumer<ProductoUbicacionChangedEvent> onProductoUbicacionChanged() {
        return event -> {
            try {
                procesarCambioUbicacion(event);
            } catch (Exception e) {
                log.error("[Inventario] Error en onProductoUbicacionChanged — idEstante={}: {}",
                        event != null ? event.getIdEstante() : null, e.getMessage(), e);
            }
        };
    }

    private void procesarCambioUbicacion(ProductoUbicacionChangedEvent event) {
        if (event == null || event.getIdEstante() == null) return;
        if (esDuplicado(event.getEventId())) return;

        EstanteModel estante = estanteRepository.findById(event.getIdEstante()).orElse(null);
        if (estante == null) {
            log.warn("[Inventario] Estante no encontrado — idEstante={}", event.getIdEstante());
            return;
        }

        int nuevoStock = Math.max(0, (estante.getStockActual() != null ? estante.getStockActual() : 0) + event.getDelta());
        estante.setStockActual(nuevoStock);
        estanteRepository.save(estante);

        int cap = estante.getCapacidadTotal() != null ? estante.getCapacidadTotal() : capacidadDefault;
        double pct = cap > 0 ? Math.round((double) nuevoStock / cap * 1000.0) / 10.0 : 0.0;
        log.info("[Inventario] Stock actualizado — estante={} delta={} stock={} ocupacion={}%",
                estante.getCodigo(), event.getDelta(), nuevoStock, pct);

        // Persiste la ocupación calculada en el/los EstPasi del estante (el frontend lee este campo).
        List<EstPasiModel> vinculos = estPasiRepository.findByEstante_IdEstante(event.getIdEstante());
        for (EstPasiModel vinculo : vinculos) {
            vinculo.setOcupacionPct(pct);
        }
        estPasiRepository.saveAll(vinculos);

        if (pct >= umbralAlerta) {
            Long idBodega = estPasiRepository.findBodegaIdByEstanteId(event.getIdEstante()).orElse(null);
            alertaBodegaService.crearAlertaSiNoDuplicada(estante.getIdEstante(), estante.getCodigo(), idBodega, pct);
        }
    }

    private boolean esDuplicado(String eventId) {
        if (eventId == null) return false;
        if (!processedEvents.add(eventId)) {
            log.info("[Inventario] Evento duplicado descartado — eventId={}", eventId);
            return true;
        }
        if (processedEvents.size() > MAX_PROCESSED_EVENTS) {
            processedEvents.clear();
        }
        return false;
    }

    /**
     * Consume eventos de cambios en el catálogo de productos publicados por Producto.
     * Permite que Inventario reaccione ante cambios de stock, precio o estado.
     * Binding: onProductoActualizado-in-0 → producto-actualizado-topic
     */
    @Bean
    public Consumer<Map<String, Object>> onProductoActualizado() {
        return event -> {
            try {
                Object productoId   = event.get("productoId");
                Object nombre       = event.get("nombre");
                Object stock        = event.get("stock");
                Object estadoNombre = event.get("estadoNombre");
                Object tipoEvento   = event.get("tipoEvento");

                log.info("[Inventario] Producto actualizado — productoId={} nombre='{}' stock={} estado={} tipo={}",
                        productoId, nombre, stock, estadoNombre, tipoEvento);

                if ("STOCK_CAMBIADO".equals(tipoEvento) || "DESACTIVADO".equals(tipoEvento)) {
                    log.info("[Inventario] Cambio relevante en producto={} — tipo={} stockActual={}",
                            productoId, tipoEvento, stock);
                }
            } catch (Exception e) {
                log.error("[Inventario] Error procesando onProductoActualizado: {}", e.getMessage(), e);
            }
        };
    }
}
