package Rol.example.Rol.client;

import Rol.example.Rol.dto.UserDto;
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
// Usa Circuit Breaker: si Users no responde, devuelve lista vacía en lugar de propagar el error.
@Component
public class UsersClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${users.service.url}")
    private String usersUrl;

    public List<UserDto> getUsuariosByRol(UUID rolId) {
        return circuitBreakerFactory.create("usersClientRol").run(
            () -> {
                ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                    usersUrl + "/api/users/por-rol/" + rolId,
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
                );
                return response.getBody() != null ? response.getBody() : Collections.emptyList();
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Rol→Users] getUsuariosByRol: " + throwable.getMessage());
                return Collections.emptyList();
            }
        );
    }
}
