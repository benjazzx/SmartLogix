package User.example.Users.client;

import User.example.Users.dto.RolDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.UUID;

@Component
public class RolClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${rol.service.url}")
    private String rolUrl;

    public RolDto getRolById(UUID rolId) {
        return circuitBreakerFactory.create("rolClient").run(
            () -> restTemplate.getForObject(rolUrl + "/api/roles/" + rolId, RolDto.class),
            throwable -> {
                System.err.println("[CircuitBreaker][Users→Rol] getRolById: " + throwable.getMessage());
                throw new RuntimeException("Servicio Rol no disponible");
            }
        );
    }

    public RolDto getRolByNombre(String nombre) {
        return circuitBreakerFactory.create("rolClient").run(
            () -> {
                RolDto[] roles = restTemplate.getForObject(rolUrl + "/api/roles", RolDto[].class);
                if (roles == null) return null;
                return Arrays.stream(roles)
                        .filter(r -> nombre.equals(r.getNombre()))
                        .findFirst()
                        .orElse(null);
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Users→Rol] getRolByNombre: " + throwable.getMessage());
                return null;
            }
        );
    }
}
