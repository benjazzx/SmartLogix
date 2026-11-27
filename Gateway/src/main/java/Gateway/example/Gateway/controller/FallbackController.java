package Gateway.example.Gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, String>> usersFallback() {
        return noDisponible("Users", "users-service");
    }

    @RequestMapping(value = "/users", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> usersFallbackEscritura() {
        return noDisponible("Users", "users-service");
    }

    @GetMapping("/rol")
    public ResponseEntity<Map<String, String>> rolFallback() {
        return noDisponible("Rol", "rol-service");
    }

    @RequestMapping(value = "/rol", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> rolFallbackEscritura() {
        return noDisponible("Rol", "rol-service");
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, String>> estadoFallback() {
        return noDisponible("Estado", "estado-service");
    }

    @RequestMapping(value = "/estado", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> estadoFallbackEscritura() {
        return noDisponible("Estado", "estado-service");
    }

    @GetMapping("/inventario")
    public ResponseEntity<Map<String, String>> inventarioFallback() {
        return noDisponible("Inventario", "inventario-service");
    }

    @RequestMapping(value = "/inventario", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> inventarioFallbackEscritura() {
        return noDisponible("Inventario", "inventario-service");
    }

    @GetMapping("/orden")
    public ResponseEntity<Map<String, String>> ordenFallback() {
        return noDisponible("Orden", "orden-service");
    }

    @RequestMapping(value = "/orden", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> ordenFallbackEscritura() {
        return noDisponible("Orden", "orden-service");
    }

    @GetMapping("/producto")
    public ResponseEntity<Map<String, String>> productoFallback() {
        return noDisponible("Producto", "producto-service");
    }

    @RequestMapping(value = "/producto", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> productoFallbackEscritura() {
        return noDisponible("Producto", "producto-service");
    }

    @GetMapping("/configuracion")
    public ResponseEntity<Map<String, String>> configuracionFallback() {
        return noDisponible("Configuracion", "configuracion-service");
    }

    @RequestMapping(value = "/configuracion", method = {RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> configuracionFallbackEscritura() {
        return noDisponible("Configuracion", "configuracion-service");
    }

    private ResponseEntity<Map<String, String>> noDisponible(String nombre, String servicio) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Servicio " + nombre + " no disponible. Intente más tarde.", "service", servicio));
    }
}
