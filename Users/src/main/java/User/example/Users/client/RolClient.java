package User.example.Users.client;

import User.example.Users.dto.RolDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

// Cliente HTTP hacia el microservicio Rol.
// Se usa para validar que un rol existe antes de asignarlo a un usuario.
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
}
