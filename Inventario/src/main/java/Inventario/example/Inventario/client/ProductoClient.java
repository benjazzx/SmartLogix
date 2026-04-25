package Inventario.example.Inventario.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente hacia el microservicio Producto usando Circuit Breaker Resilience4j.
 * Si Producto no responde, el CB devuelve un fallback para no bloquear el inventario.
 */
@Component
public class ProductoClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${producto.service.url}")
    private String productoUrl;

    /**
     * Verifica que un producto exista en el catálogo antes de asociarlo a una ubicación.
     * Fallback: retorna true (no se bloquea la operación si Producto no responde).
     */
    public boolean existeProducto(UUID productoId) {
        return circuitBreakerFactory.create("productoClient").run(
            () -> {
                Map<?, ?> producto = restTemplate.getForObject(
                    productoUrl + "/api/productos/" + productoId, Map.class);
                return producto != null;
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Inventario→Producto] existeProducto: " + throwable.getMessage());
                // Degradación: si Producto no responde se acepta la operación
                return true;
            }
        );
    }
}
