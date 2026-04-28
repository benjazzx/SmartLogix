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
                log.error("[CircuitBreaker][Orden→Estado] existeEstado estadoId={}: {}", estadoId, throwable.getMessage());
                return true;
            }
        );
    }
}
