package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.BodegaRequestDTO;
import Inventario.example.Inventario.dto.BodegaResponseDTO;
import Inventario.example.Inventario.service.BodegaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/bodegas")
@RequiredArgsConstructor
public class BodegaController {

    private final BodegaService bodegaService;

    @GetMapping
    public ResponseEntity<List<BodegaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(bodegaService.listarTodas());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<BodegaResponseDTO>> listarActivas() {
        return ResponseEntity.ok(bodegaService.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BodegaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<BodegaResponseDTO> crear(@Valid @RequestBody BodegaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bodegaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BodegaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BodegaRequestDTO dto) {
        return ResponseEntity.ok(bodegaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        bodegaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
