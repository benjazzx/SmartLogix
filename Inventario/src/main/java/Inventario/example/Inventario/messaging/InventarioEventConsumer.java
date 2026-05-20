package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.client.ProductoClient;
import Inventario.example.Inventario.dto.OrdenCreadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioEventConsumer {

    private final ProductoClient productoClient;

    @Bean
    public Consumer<OrdenCreadaEvent> onOrdenCreada() {
        return event -> {
            log.info("[Inventario] Orden recibida — ordenId={} userId={}", event.getOrdenId(), event.getUserId());
            if (event.getDetalles() != null) {
                event.getDetalles().forEach(this::procesarDetalle);
            }
        };
    }

    private void procesarDetalle(OrdenCreadaEvent.DetalleDto detalle) {
        try {
            if (detalle.getProductoId() == null || detalle.getCantidad() == null) return;
            boolean existe = productoClient.existeProducto(detalle.getProductoId());
            log.info("[Inventario] Producto productoId={} cantidad={} existe={}",
                    detalle.getProductoId(), detalle.getCantidad(), existe);
            if (!existe) {
                log.warn("[Inventario] Producto no encontrado en catálogo — productoId={}", detalle.getProductoId());
                return;
            }
            boolean ok = productoClient.decrementarStock(detalle.getProductoId(), detalle.getCantidad());
            if (!ok) {
                log.warn("[Inventario] No se pudo decrementar stock — productoId={} cantidad={}",
                        detalle.getProductoId(), detalle.getCantidad());
            }
        } catch (Exception e) {
            log.error("[Inventario] Error procesando detalle productoId={}: {}", detalle.getProductoId(), e.getMessage());
        }
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
