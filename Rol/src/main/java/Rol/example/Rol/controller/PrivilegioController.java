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

import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.service.PrivilegioService;

@RestController
@RequestMapping("/api/privilegios")
@Tag(name = "Privilegios", description = "Acciones específicas que puede realizar un rol en el sistema")
public class PrivilegioController {

    @Autowired
    private PrivilegioService privilegioService;

    @Operation(summary = "Listar todos los privilegios")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<PrivilegioModel> getAllPrivilegios() {
        return privilegioService.getAllPrivilegios();
    }

    @Operation(summary = "Obtener privilegio por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Privilegio encontrado"),
        @ApiResponse(responseCode = "404", description = "Privilegio no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PrivilegioModel> getPrivilegioById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(privilegioService.getPrivilegioById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Listar privilegios por tipo")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/por-tipo/{tipoId}")
    public ResponseEntity<List<PrivilegioModel>> getPrivilegiosByTipo(
            @Parameter(description = "UUID del tipo") @PathVariable UUID tipoId) {
        try {
            return ResponseEntity.ok(privilegioService.getPrivilegiosByTipo(tipoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear nuevo privilegio")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Privilegio creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Nombre duplicado o tipo inválido")
    })
    @PostMapping
    public ResponseEntity<PrivilegioModel> createPrivilegio(@RequestBody PrivilegioModel privilegio) {
        try {
            return ResponseEntity.ok(privilegioService.createPrivilegio(privilegio));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Actualizar privilegio existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Privilegio actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Privilegio no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PrivilegioModel> updatePrivilegio(@PathVariable UUID id, @RequestBody PrivilegioModel privilegio) {
        try {
            return ResponseEntity.ok(privilegioService.updatePrivilegio(id, privilegio));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar privilegio por ID")
    @ApiResponse(responseCode = "204", description = "Privilegio eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrivilegio(@PathVariable UUID id) {
        try {
            privilegioService.deletePrivilegio(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
