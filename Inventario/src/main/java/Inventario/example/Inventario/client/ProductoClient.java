package Inventario.example.Inventario.client;

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

    @Value("${internal.service.key}")
    private String internalServiceKey;

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

    public boolean decrementarStock(UUID productoId, int cantidad, Long ordenId,
                                     UUID compradorId, String compradorNombre) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                String url = UriComponentsBuilder.fromUriString(productoUrl + "/api/productos/" + productoId + "/decrementar-stock")
                        .queryParam("cantidad", cantidad)
                        .queryParamIfPresent("ordenId", java.util.Optional.ofNullable(ordenId))
                        .queryParamIfPresent("compradorId", java.util.Optional.ofNullable(compradorId))
                        .queryParamIfPresent("compradorNombre", java.util.Optional.ofNullable(compradorNombre))
                        .toUriString();
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Internal-Key", internalServiceKey);
                restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
                log.info("[Inventario→Producto] Stock decrementado — productoId={} cantidad={} ordenId={}",
                        productoId, cantidad, ordenId);
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
