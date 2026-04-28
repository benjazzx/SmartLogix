package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.client.ProductoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Consumidor Kafka orientado a eventos del ecosistema SmartLogix.
 *
 * Tópicos consumidos:
 *   - orden-creada-topic   → publicado por Orden cuando se crea una nueva orden
 *   - producto-actualizado-topic → publicado por Producto ante cambios de stock/precio/estado
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioEventConsumer {

    private final ProductoClient productoClient;

    /**
     * Consume eventos de órdenes creadas.
     * Verifica existencia de cada producto vía ProductoClient (Circuit Breaker).
     * Binding: onOrdenCreada-in-0 → orden-creada-topic
     */
    @Bean
    public Consumer<Map<String, Object>> onOrdenCreada() {
        return event -> {
            try {
                Object ordenId = event.get("ordenId");
                Object userId  = event.get("userId");
                log.info("[Inventario] Orden recibida — ordenId={} userId={}", ordenId, userId);

                Object detallesObj = event.get("detalles");
                if (detallesObj instanceof List<?> detalles) {
                    for (Object item : detalles) {
                        if (item instanceof Map<?, ?> detalle) {
                            Object productoIdRaw = detalle.get("productoId");
                            Object cantidad      = detalle.get("cantidad");
                            if (productoIdRaw != null) {
                                UUID productoId = UUID.fromString(productoIdRaw.toString());
                                boolean existe = productoClient.existeProducto(productoId);
                                log.info("[Inventario] Validando producto productoId={} cantidad={} existe={}",
                                        productoId, cantidad, existe);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[Inventario] Error procesando onOrdenCreada: {}", e.getMessage(), e);
            }
        };
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
