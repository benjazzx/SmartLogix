package Orden.example.Orden.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

// Llama al microservicio Users para obtener datos del usuario (nombre para desnormalizar).
// Si Users no responde, el Circuit Breaker devuelve null y la orden se crea igual —
// el userId ya viene validado por el JWT, asi que la existencia del usuario es garantizada.
@Component
public class UsersClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${users.service.url}")
    private String usersUrl;

    public String getNombreUsuario(UUID userId) {
        return circuitBreakerFactory.create("usersClient").run(
            () -> {
                Map<?, ?> user = restTemplate.getForObject(
                    usersUrl + "/api/users/" + userId, Map.class);
                if (user != null && user.containsKey("nombre")) {
                    return (String) user.get("nombre");
                }
                return null;
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Orden→Users] getNombreUsuario: " + throwable.getMessage());
                return null;
            }
        );
    }
}
