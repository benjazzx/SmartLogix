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

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class UsersClient {

    private static final String KEY_NOMBRE  = "nombre";
    private static final String KEY_COMUNA  = "comuna";
    private static final String KEY_CALLE   = "calle";
    private static final String KEY_NUMERO  = "numero";

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${users.service.url}")
    private String usersUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    // Endpoint interno (no requiere JWT de usuario, protegido por X-Internal-Key) — el
    // GET /api/users/{id} normal exige rol admin, que una llamada de servicio no tiene.
    private Map<?, ?> consultarUsuarioInterno(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Key", internalServiceKey);
        return restTemplate.exchange(
                usersUrl + "/api/users/" + userId + "/interno",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class
        ).getBody();
    }

    public String getNombreUsuario(UUID userId) {
        return circuitBreakerFactory.create("usersClient").run(
            () -> {
                Map<?, ?> user = consultarUsuarioInterno(userId);
                if (user != null && user.containsKey(KEY_NOMBRE)) {
                    return (String) user.get(KEY_NOMBRE);
                }
                return null;
            },
            throwable -> {
                log.error("[CircuitBreaker][Orden→Users] getNombreUsuario userId={}: {}", userId, throwable.getMessage());
                return null;
            }
        );
    }

    public String getDireccionTexto(UUID userId) {
        return circuitBreakerFactory.create("usersClientDir").run(
            () -> {
                Map<?, ?> user = consultarUsuarioInterno(userId);
                if (user == null) return null;
                Object dirObj = user.get("direccion");
                if (!(dirObj instanceof Map<?, ?> dir)) return null;
                String calle  = dir.get(KEY_CALLE)  instanceof String s ? s : "";
                String numero = dir.get(KEY_NUMERO) instanceof String s ? s : "";
                Object comunaObj = dir.get(KEY_COMUNA);
                String ciudad = "";
                if (comunaObj instanceof Map<?, ?> comuna) {
                    ciudad = comuna.get(KEY_NOMBRE) instanceof String s ? s : "";
                }
                return (calle + " " + numero + (ciudad.isEmpty() ? "" : ", " + ciudad)).trim();
            },
            throwable -> {
                log.error("[CircuitBreaker][Orden→Users] getDireccionTexto userId={}: {}", userId, throwable.getMessage());
                return null;
            }
        );
    }
}
