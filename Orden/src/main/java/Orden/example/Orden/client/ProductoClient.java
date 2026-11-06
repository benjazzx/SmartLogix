package Orden.example.Orden.client;

import Orden.example.Orden.exception.ServicioOcupadoException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

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
                if (esFalloTransitorio(throwable)) {
                    throw new ServicioOcupadoException(
                        "El servicio de productos está temporalmente saturado. Intenta de nuevo en unos segundos.");
                }
                // 404 u otro rechazo real del producto: no es transitorio, es un "no existe".
                return null;
            }
        );
    }

    // Reserva atómica de stock (check + descuento en el mismo UPDATE de Producto). Devuelve false
    // solo cuando Producto confirma (409) que no hay stock suficiente — un rechazo de negocio real.
    // Si la causa es el circuit breaker abierto, un timeout o la conexión caída, se lanza
    // ServicioOcupadoException en vez de reportarlo como si el stock no alcanzara.
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
                if (esRechazoDeStockReal(throwable)) {
                    return false;
                }
                throw new ServicioOcupadoException(
                    "El servicio de productos está temporalmente saturado. Intenta de nuevo en unos segundos.");
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

    // El circuit breaker abierto, un timeout o la conexión caída son fallas del sistema,
    // no una respuesta real de Producto — no deben confundirse con "no existe" o "sin stock".
    private boolean esFalloTransitorio(Throwable t) {
        if (t instanceof CallNotPermittedException || t instanceof TimeoutException
                || t instanceof ResourceAccessException) {
            return true;
        }
        if (t instanceof HttpStatusCodeException httpEx) {
            return httpEx.getStatusCode().is5xxServerError();
        }
        return false;
    }

    // Único caso en que Producto confirma, con su propia respuesta (409), que de verdad
    // no hay stock suficiente — cualquier otra causa es una falla transitoria del sistema.
    private boolean esRechazoDeStockReal(Throwable t) {
        return t instanceof HttpStatusCodeException httpEx
                && httpEx.getStatusCode() == HttpStatusCode.valueOf(409);
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
