package Rol.example.Rol.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.service.PermisoService;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Asignación de privilegios a roles — define qué puede hacer cada rol")
public class PermisoController {

    @Autowired
    private PermisoService permisoService;

    @Operation(summary = "Listar todos los permisos")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<PermisoModel> getAllPermisos() {
        return permisoService.getAllPermisos();
    }

    @Operation(summary = "Listar permisos de un rol específico")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/por-rol/{rolId}")
    public ResponseEntity<List<PermisoModel>> getPermisosByRol(
            @Parameter(description = "UUID del rol") @PathVariable UUID rolId) {
        try {
            return ResponseEntity.ok(permisoService.getPermisosByRol(rolId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Verificar si un rol tiene un privilegio específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "true si tiene el permiso, false si no")
    })
    @GetMapping("/verificar")
    public ResponseEntity<Boolean> tienePermiso(
            @Parameter(description = "UUID del rol") @RequestParam UUID rolId,
            @Parameter(description = "UUID del privilegio") @RequestParam UUID privilegioId) {
        return ResponseEntity.ok(permisoService.tienePermiso(rolId, privilegioId));
    }

    @Operation(summary = "Asignar un privilegio a un rol")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permiso asignado correctamente"),
        @ApiResponse(responseCode = "400", description = "Rol o privilegio inválido, o ya asignado")
    })
    @PostMapping("/asignar")
    public ResponseEntity<PermisoModel> asignarPermiso(
            @Parameter(description = "UUID del rol") @RequestParam UUID rolId,
            @Parameter(description = "UUID del privilegio") @RequestParam UUID privilegioId) {
        try {
            return ResponseEntity.ok(permisoService.asignarPermiso(rolId, privilegioId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Revocar un privilegio de un rol")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Permiso revocado correctamente"),
        @ApiResponse(responseCode = "400", description = "El rol no tiene ese privilegio")
    })
    @DeleteMapping("/revocar")
    public ResponseEntity<Void> revocarPermiso(
            @Parameter(description = "UUID del rol") @RequestParam UUID rolId,
            @Parameter(description = "UUID del privilegio") @RequestParam UUID privilegioId) {
        try {
            permisoService.revocarPermiso(rolId, privilegioId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Eliminar permiso por ID")
    @ApiResponse(responseCode = "204", description = "Permiso eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermiso(@PathVariable UUID id) {
        try {
            permisoService.deletePermiso(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
