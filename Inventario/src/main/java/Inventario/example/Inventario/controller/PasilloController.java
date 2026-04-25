package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.dto.PasilloRequestDTO;
import Inventario.example.Inventario.dto.PasilloResponseDTO;
import Inventario.example.Inventario.service.PasilloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/pasillos")
@RequiredArgsConstructor
public class PasilloController {

    private final PasilloService pasilloService;

    @GetMapping
    public ResponseEntity<List<PasilloResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pasilloService.listarTodos());
    }

    @GetMapping("/bodega/{idBodega}")
    public ResponseEntity<List<PasilloResponseDTO>> listarPorBodega(@PathVariable Long idBodega) {
        return ResponseEntity.ok(pasilloService.listarPorBodega(idBodega));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PasilloResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pasilloService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PasilloResponseDTO> crear(@Valid @RequestBody PasilloRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pasilloService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PasilloResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PasilloRequestDTO dto) {
        return ResponseEntity.ok(pasilloService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pasilloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
