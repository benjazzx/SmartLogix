package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.EstanteRequestDTO;
import Inventario.example.Inventario.dto.EstanteResponseDTO;
import Inventario.example.Inventario.service.EstanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/estantes")
@RequiredArgsConstructor
public class EstanteController {

    private final EstanteService estanteService;

    @GetMapping
    public ResponseEntity<List<EstanteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estanteService.listarTodos());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<EstanteResponseDTO>> listarActivos() {
        return ResponseEntity.ok(estanteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstanteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estanteService.obtenerPorId(id));
    }

    @GetMapping("/por-pasillo/{idPasillo}")
    public ResponseEntity<List<EstanteResponseDTO>> listarPorPasillo(@PathVariable Long idPasillo) {
        return ResponseEntity.ok(estanteService.listarPorPasillo(idPasillo));
    }

    @GetMapping("/por-bodega/{idBodega}")
    public ResponseEntity<List<EstanteResponseDTO>> listarPorBodega(@PathVariable Long idBodega) {
        return ResponseEntity.ok(estanteService.listarPorBodega(idBodega));
    }

    @PostMapping
    public ResponseEntity<EstanteResponseDTO> crear(@Valid @RequestBody EstanteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estanteService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstanteResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EstanteRequestDTO dto) {
        return ResponseEntity.ok(estanteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
