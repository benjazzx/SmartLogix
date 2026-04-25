package Orden.example.Orden.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

// Valida que el estadoId exista en el microservicio Estado antes de agregarlo al historial.
@Component
public class EstadoClient {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value("${estado.service.url}")
    private String estadoUrl;

    public boolean existeEstado(UUID estadoId) {
        return circuitBreakerFactory.create("estadoClient").run(
            () -> {
                Map<?, ?> estado = restTemplate.getForObject(
                    estadoUrl + "/api/estados/" + estadoId, Map.class);
                return estado != null;
            },
            throwable -> {
                System.err.println("[CircuitBreaker][Orden→Estado] existeEstado: " + throwable.getMessage());
                // Degradacion: si Estado no responde, se acepta el estado del historial
                return true;
            }
        );
    }
}
