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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import Rol.example.Rol.client.UsersClient;
import Rol.example.Rol.dto.UserDto;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Gestión de roles de usuario en SmartLogix")
public class RolController {

    @Autowired
    private RolService rolService;

    @Autowired
    private UsersClient usersClient;

    @Operation(summary = "Listar todos los roles")
    @ApiResponse(responseCode = "200", description = "Lista de roles obtenida correctamente")
    @GetMapping
    public List<RolModel> getAllRoles() {
        return rolService.getAllRoles();
    }

    @Operation(summary = "Obtener rol por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol encontrado"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RolModel> getRolById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(rolService.getRolById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Asignar rol según dominio de email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol asignado correctamente"),
        @ApiResponse(responseCode = "200", description = "Rol de emergencia asignado (circuit breaker activo)")
    })
    @CircuitBreaker(name = "rolService", fallbackMethod = "fallbackAssignRole")
    @GetMapping("/assign")
    public ResponseEntity<RolModel> assignRoleByEmail(
            @Parameter(description = "Email del usuario a asignar rol", example = "juan@smartb.cl")
            @RequestParam String email) {
        return ResponseEntity.ok(rolService.assignRoleByEmail(email));
    }

    public ResponseEntity<RolModel> fallbackAssignRole(String email, Throwable exception) {
        RolModel fallbackRol = new RolModel();
        fallbackRol.setId(UUID.randomUUID());
        fallbackRol.setNombre("cliente_temporal_por_falla");
        fallbackRol.setDescripcion("Rol de emergencia asignado automáticamente al caer el servicio principal.");
        return ResponseEntity.ok(fallbackRol);
    }

    @Operation(summary = "Crear nuevo rol")
    @ApiResponse(responseCode = "200", description = "Rol creado correctamente")
    @PostMapping
    public RolModel createRol(@RequestBody RolModel rol) {
        return rolService.createRol(rol);
    }

    @Operation(summary = "Actualizar rol existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RolModel> updateRol(@PathVariable UUID id, @RequestBody RolModel rol) {
        try {
            return ResponseEntity.ok(rolService.updateRol(id, rol));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Listar usuarios que tienen un rol específico")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida del microservicio Users")
    @GetMapping("/{id}/usuarios")
    public ResponseEntity<List<UserDto>> getUsuariosByRol(
            @Parameter(description = "UUID del rol") @PathVariable UUID id) {
        return ResponseEntity.ok(usersClient.getUsuariosByRol(id));
    }

    @Operation(summary = "Eliminar rol por ID")
    @ApiResponse(responseCode = "204", description = "Rol eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRol(@PathVariable UUID id) {
        rolService.deleteRol(id);
        return ResponseEntity.noContent().build();
    }
}
