package Orden.example.Orden.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    public static String extraerNombre(Map<String, Object> p) {
        return p != null && p.get("nombre") instanceof String s ? s : null;
    }

    public static BigDecimal extraerPrecio(Map<String, Object> p) {
        if (p == null) return null;
        Object val = p.get("precio");
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return null;
    }
}
