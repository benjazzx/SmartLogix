package Inventario.example.Inventario.controller;

import Inventario.example.Inventario.model.AlertaBodegaModel;
import Inventario.example.Inventario.service.AlertaBodegaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/alertas")
@RequiredArgsConstructor
public class AlertaBodegaController {

    private final AlertaBodegaService alertaBodegaService;

    @GetMapping
    public ResponseEntity<List<AlertaBodegaModel>> listarNoLeidas() {
        return ResponseEntity.ok(alertaBodegaService.listarNoLeidas());
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        alertaBodegaService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }
}
