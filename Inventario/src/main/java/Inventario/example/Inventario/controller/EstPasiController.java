package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.EstPasiRequestDTO;
import Inventario.example.Inventario.dto.EstPasiResponseDTO;
import Inventario.example.Inventario.service.EstPasiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario/est-pasi")
@RequiredArgsConstructor
public class EstPasiController {

    private final EstPasiService estPasiService;

    @GetMapping
    public ResponseEntity<List<EstPasiResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estPasiService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstPasiResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estPasiService.obtenerPorId(id));
    }

    @GetMapping("/por-pasillo/{idPasillo}")
    public ResponseEntity<List<EstPasiResponseDTO>> listarPorPasillo(@PathVariable Long idPasillo) {
        return ResponseEntity.ok(estPasiService.listarPorPasillo(idPasillo));
    }

    @GetMapping("/por-estante/{idEstante}")
    public ResponseEntity<List<EstPasiResponseDTO>> listarPorEstante(@PathVariable Long idEstante) {
        return ResponseEntity.ok(estPasiService.listarPorEstante(idEstante));
    }

    @GetMapping("/por-bodega/{idBodega}")
    public ResponseEntity<List<EstPasiResponseDTO>> listarPorBodega(@PathVariable Long idBodega) {
        return ResponseEntity.ok(estPasiService.listarPorBodega(idBodega));
    }

    @GetMapping("/ocupacion/bodega/{idBodega}")
    public ResponseEntity<Map<String, Double>> calcularOcupacionBodega(@PathVariable Long idBodega) {
        Double pct = estPasiService.calcularOcupacionPromedioBodega(idBodega);
        return ResponseEntity.ok(Map.of("ocupacionPromedioPct", pct));
    }

    @PostMapping
    public ResponseEntity<EstPasiResponseDTO> crear(@Valid @RequestBody EstPasiRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estPasiService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstPasiResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EstPasiRequestDTO dto) {
        return ResponseEntity.ok(estPasiService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estPasiService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
