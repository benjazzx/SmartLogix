package Rol.example.Rol.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolService rolService;

    @GetMapping
    public List<RolModel> getAllRoles() {
        return rolService.getAllRoles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolModel> getRolById(@PathVariable UUID id) {
        RolModel rol = rolService.getRolById(id);
        if (rol != null) {
            return ResponseEntity.ok(rol);
        }
        return ResponseEntity.notFound().build();
    }

    @CircuitBreaker(name = "rolService", fallbackMethod = "fallbackAssignRole")
    @GetMapping("/assign")
    public ResponseEntity<RolModel> assignRoleByEmail(@RequestParam String email) {
        RolModel rol = rolService.assignRoleByEmail(email);
        if (rol != null) {
            return ResponseEntity.ok(rol);
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<RolModel> fallbackAssignRole(String email, Throwable exception) {
        System.out.println("⚠️ CIRCUIT BREAKER ACTIVO: Ejecutando Plan B para email: " + email);
        System.out.println("Error prevenido gracias a Resilience4j: " + exception.getMessage());
        
        RolModel fallbackRol = new RolModel();
        fallbackRol.setId(UUID.randomUUID());
        fallbackRol.setNombre("cliente_temporal_por_falla");
        fallbackRol.setDescripcion("Rol de emergencia asignado automáticamente al caer el servicio principal.");
        
        return ResponseEntity.ok(fallbackRol);
    }

    @PostMapping
    public RolModel createRol(@RequestBody RolModel rol) {
        return rolService.createRol(rol);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RolModel> updateRol(@PathVariable UUID id, @RequestBody RolModel rol) {
        RolModel updatedRol = rolService.updateRol(id, rol);
        if (updatedRol != null) {
            return ResponseEntity.ok(updatedRol);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRol(@PathVariable UUID id) {
        rolService.deleteRol(id);
        return ResponseEntity.noContent().build();
    }
}
