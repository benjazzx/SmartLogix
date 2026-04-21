package Estado.example.Estado.Controller;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Service.EstadoService;

@RestController
@RequestMapping("/api/estados")
@Tag(name = "Estados", description = "Gestión de estados de usuario en SmartLogix")
public class EstadoController {

    @Autowired
    private EstadoService estadoService;

    @Operation(summary = "Listar todos los estados")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<Estado> getAll() {
        return estadoService.getAll();
    }

    @Operation(summary = "Obtener estado por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado encontrado"),
        @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Estado> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(estadoService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Asignar estado según rol del usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado asignado correctamente"),
        @ApiResponse(responseCode = "200", description = "Estado de emergencia asignado (circuit breaker activo)")
    })
    @CircuitBreaker(name = "estadoService", fallbackMethod = "fallbackAssignEstado")
    @GetMapping("/assign")
    public ResponseEntity<Estado> assignEstadoByRol(
            @Parameter(description = "Nombre del rol asignado al usuario", example = "bodeguero")
            @RequestParam String roleName) {
        return ResponseEntity.ok(estadoService.assignEstadoByRol(roleName));
    }

    public ResponseEntity<Estado> fallbackAssignEstado(String roleName, Throwable exception) {
        Estado fallback = new Estado();
        fallback.setId(UUID.randomUUID());
        fallback.setNombre("pendiente_verificacion_temporal");
        fallback.setDescripcion("Estado de emergencia asignado automáticamente al caer el servicio principal.");
        return ResponseEntity.ok(fallback);
    }

    @Operation(summary = "Listar estados por tipo")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/tipo/{tipoNombre}")
    public List<Estado> getByTipo(@PathVariable String tipoNombre) {
        return estadoService.getByTipoNombre(tipoNombre);
    }

    @Operation(summary = "Crear nuevo estado")
    @ApiResponse(responseCode = "200", description = "Estado creado correctamente")
    @PostMapping
    public Estado create(@RequestBody Estado estado) {
        return estadoService.create(estado);
    }

    @Operation(summary = "Actualizar estado existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Estado> update(@PathVariable UUID id, @RequestBody Estado estado) {
        try {
            return ResponseEntity.ok(estadoService.update(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar estado por ID")
    @ApiResponse(responseCode = "204", description = "Estado eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        estadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
