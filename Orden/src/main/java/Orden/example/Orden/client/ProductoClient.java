package Orden.example.Orden.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ProductoClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${producto.service.url}")
    private String productoUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProducto(UUID productoId) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> restTemplate.getForObject(
                productoUrl + "/api/productos/" + productoId, Map.class),
            throwable -> {
                log.error("[CircuitBreaker][Orden→Producto] getProducto productoId={}: {}",
                        productoId, throwable.getMessage());
                return null;
            }
        );
    }

    // Reserva atómica de stock (check + descuento en el mismo UPDATE de Producto). Devuelve false
    // tanto si no hay stock suficiente como si Producto no responde — en ambos casos la orden no
    // debe crearse, para no vender de más.
    public boolean reservarStock(UUID productoId, int cantidad, Long ordenId) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                String url = UriComponentsBuilder.fromUriString(productoUrl + "/api/productos/" + productoId + "/reservar-stock")
                        .queryParam("cantidad", cantidad)
                        .queryParamIfPresent("ordenId", java.util.Optional.ofNullable(ordenId))
                        .toUriString();
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Internal-Key", internalServiceKey);
                restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
                log.info("[Orden→Producto] Stock reservado — productoId={} cantidad={} ordenId={}",
                        productoId, cantidad, ordenId);
                return true;
            },
            throwable -> {
                log.error("[CircuitBreaker][Orden→Producto] reservarStock fallback — productoId={} cantidad={}: {}",
                        productoId, cantidad, throwable.getMessage());
                return false;
            }
        );
    }

    public boolean registrarDevolucion(UUID productoId, int cantidad, boolean danado, Long ordenId) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                String url = UriComponentsBuilder.fromUriString(productoUrl + "/api/productos/" + productoId + "/registrar-devolucion")
                        .queryParam("cantidad", cantidad)
                        .queryParam("danado", danado)
                        .queryParamIfPresent("ordenId", java.util.Optional.ofNullable(ordenId))
                        .toUriString();
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Internal-Key", internalServiceKey);
                restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
                log.info("[Orden→Producto] Devolución registrada — productoId={} cantidad={} danado={} ordenId={}",
                        productoId, cantidad, danado, ordenId);
                return true;
            },
            throwable -> {
                log.error("[CircuitBreaker][Orden→Producto] registrarDevolucion fallback — productoId={} cantidad={}: {}",
                        productoId, cantidad, throwable.getMessage());
                return false;
            }
        );
    }

    public static String extraerNombre(Map<String, Object> p) {
        return p != null && p.get("nombre") instanceof String s ? s : null;
    }

    public static BigDecimal extraerPrecio(Map<String, Object> p) {
        if (p == null) return null;
        Object val = p.get("precio");
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return null;
    }

    public static Integer extraerStock(Map<String, Object> p) {
        if (p == null) return null;
        Object val = p.get("stock");
        if (val instanceof Number n) return n.intValue();
        return null;
    }
}
