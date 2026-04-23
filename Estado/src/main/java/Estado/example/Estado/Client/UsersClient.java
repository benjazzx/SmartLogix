package Estado.example.Estado.Client;

import Estado.example.Estado.Dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Cliente HTTP hacia el microservicio Users.
// Permite a Estado consultar qué usuarios tienen un estado específico.
@Component
public class UsersClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${users.service.url}")
    private String usersUrl;

    public List<UserDto> getUsuariosByEstado(UUID estadoId) {
        return circuitBreakerFactory.create("usersClientEstado").run(
            () -> {
                ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                    usersUrl + "/api/users/por-estado/" + estadoId,
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
                );
                return response.getBody() != null ? response.getBody() : Collections.emptyList();
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Estado→Users] getUsuariosByEstado: " + throwable.getMessage());
                return Collections.emptyList();
            }
        );
    }
}
