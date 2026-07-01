package Configuracion.example.Configuracion.controller;

import Configuracion.example.Configuracion.service.ConfiguracionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/config")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping("/preferencias")
    public ResponseEntity<Map<String, String>> getPreferencias(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getPreferencias(userId));
    }

    @PutMapping("/preferencias/{clave}")
    public ResponseEntity<Void> setPreferencia(
            HttpServletRequest request,
            @PathVariable String clave,
            @RequestBody Map<String, String> body) {
        UUID userId = (UUID) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        String valor = body.get("valor");
        if (valor == null || valor.isBlank()) return ResponseEntity.badRequest().build();
        service.setPreferencia(userId, clave, valor);
        return ResponseEntity.ok().build();
    }
}
