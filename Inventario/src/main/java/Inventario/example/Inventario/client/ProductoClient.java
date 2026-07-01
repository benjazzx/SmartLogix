package Inventario.example.Inventario.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente hacia el microservicio Producto usando Circuit Breaker Resilience4j.
 * Si Producto no responde, el CB devuelve un fallback para no bloquear el inventario.
 */
@Slf4j
@Component
public class ProductoClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${producto.service.url}")
    private String productoUrl;

    public boolean existeProducto(UUID productoId) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                Map<?, ?> producto = restTemplate.getForObject(
                    productoUrl + "/api/productos/" + productoId, Map.class);
                return producto != null;
            },
            throwable -> {
                log.warn("[CircuitBreaker][Inventario→Producto] existeProducto fallback: {}", throwable.getMessage());
                return true;
            }
        );
    }

    public boolean decrementarStock(UUID productoId, int cantidad) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                String url = productoUrl + "/api/productos/" + productoId
                        + "/decrementar-stock?cantidad=" + cantidad;
                restTemplate.exchange(url, HttpMethod.PATCH, null, Map.class);
                log.info("[Inventario→Producto] Stock decrementado — productoId={} cantidad={}", productoId, cantidad);
                return true;
            },
            throwable -> {
                log.error("[CircuitBreaker][Inventario→Producto] decrementarStock fallback — productoId={} cantidad={}: {}",
                        productoId, cantidad, throwable.getMessage());
                return false;
            }
        );
    }
}
