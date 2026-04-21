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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Service.TipoDeEstadoService;

@RestController
@RequestMapping("/api/tipos-estado")
@Tag(name = "Tipos de Estado", description = "Gestión de tipos de estado en SmartLogix")
public class TipoDeEstadoController {

    @Autowired
    private TipoDeEstadoService tipoDeEstadoService;

    @Operation(summary = "Listar todos los tipos de estado")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<TipoDeEstadoModel> getAll() {
        return tipoDeEstadoService.getAll();
    }

    @Operation(summary = "Obtener tipo de estado por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de estado encontrado"),
        @ApiResponse(responseCode = "404", description = "Tipo de estado no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoDeEstadoModel> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(tipoDeEstadoService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear nuevo tipo de estado")
    @ApiResponse(responseCode = "200", description = "Tipo de estado creado correctamente")
    @PostMapping
    public TipoDeEstadoModel create(@RequestBody TipoDeEstadoModel tipo) {
        return tipoDeEstadoService.create(tipo);
    }

    @Operation(summary = "Actualizar tipo de estado existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de estado actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Tipo de estado no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TipoDeEstadoModel> update(@PathVariable UUID id, @RequestBody TipoDeEstadoModel tipo) {
        try {
            return ResponseEntity.ok(tipoDeEstadoService.update(id, tipo));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar tipo de estado por ID")
    @ApiResponse(responseCode = "204", description = "Tipo de estado eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tipoDeEstadoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
