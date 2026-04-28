package Orden.example.Orden.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
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
                log.error("[CircuitBreaker][Orden→Users] getNombreUsuario userId={}: {}", userId, throwable.getMessage());
                return null;
            }
        );
    }
}
