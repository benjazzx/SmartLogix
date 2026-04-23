package Rol.example.Rol.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.service.TipoService;

@RestController
@RequestMapping("/api/tipos")
@Tag(name = "Tipos", description = "Categorías que agrupan los privilegios del sistema")
public class TipoController {

    @Autowired
    private TipoService tipoService;

    @Operation(summary = "Listar todos los tipos")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<TipoModel> getAllTipos() {
        return tipoService.getAllTipos();
    }

    @Operation(summary = "Obtener tipo por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo encontrado"),
        @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoModel> getTipoById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(tipoService.getTipoById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear nuevo tipo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Ya existe un tipo con ese nombre")
    })
    @PostMapping
    public ResponseEntity<TipoModel> createTipo(@RequestBody TipoModel tipo) {
        try {
            return ResponseEntity.ok(tipoService.createTipo(tipo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Actualizar tipo existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TipoModel> updateTipo(@PathVariable UUID id, @RequestBody TipoModel tipo) {
        try {
            return ResponseEntity.ok(tipoService.updateTipo(id, tipo));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar tipo por ID")
    @ApiResponse(responseCode = "204", description = "Tipo eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTipo(@PathVariable UUID id) {
        try {
            tipoService.deleteTipo(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
