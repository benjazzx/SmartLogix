package Producto.example.Producto.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioClient {

    private final RestTemplate restTemplate;

    @Value("${inventario.service.url:http://localhost:8083}")
    private String inventarioUrl;

    public boolean isEstanteActivo(Long idEstante) {
        try {
            String url = inventarioUrl + "/api/inventario/estantes/" + idEstante;
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) {
                throw new RuntimeException("Respuesta vacía de Inventario para estante: " + idEstante);
            }
            return Boolean.TRUE.equals(body.get("activo"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[InventarioClient] Error al verificar estante {}: {}", idEstante, e.getMessage());
            throw new RuntimeException("Inventario no disponible — no se puede validar el estante: " + idEstante, e);
        }
    }
}
