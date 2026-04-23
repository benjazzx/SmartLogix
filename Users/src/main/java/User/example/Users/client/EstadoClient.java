package User.example.Users.client;

import User.example.Users.dto.EstadoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

// Cliente HTTP hacia el microservicio Estado.
// Se usa para obtener información de un estado antes de asignarlo o validarlo.
@Component
public class EstadoClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${estado.service.url}")
    private String estadoUrl;

    public EstadoDto getEstadoById(UUID estadoId) {
        return circuitBreakerFactory.create("estadoClient").run(
            () -> restTemplate.getForObject(estadoUrl + "/api/estados/" + estadoId, EstadoDto.class),
            throwable -> {
                System.err.println("[CircuitBreaker][Users→Estado] getEstadoById: " + throwable.getMessage());
                throw new RuntimeException("Servicio Estado no disponible");
            }
        );
    }
}
