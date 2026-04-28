package Gateway.example.Gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, String>> usersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Users no disponible. Intente más tarde.", "service", "users-service"));
    }

    @GetMapping("/rol")
    public ResponseEntity<Map<String, String>> rolFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Rol no disponible. Intente más tarde.", "service", "rol-service"));
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, String>> estadoFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Estado no disponible. Intente más tarde.", "service", "estado-service"));
    }

    @GetMapping("/inventario")
    public ResponseEntity<Map<String, String>> inventarioFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Inventario no disponible. Intente más tarde.", "service", "inventario-service"));
    }

    @GetMapping("/orden")
    public ResponseEntity<Map<String, String>> ordenFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Orden no disponible. Intente más tarde.", "service", "orden-service"));
    }

    @GetMapping("/producto")
    public ResponseEntity<Map<String, String>> productoFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio Producto no disponible. Intente más tarde.", "service", "producto-service"));
    }
}
