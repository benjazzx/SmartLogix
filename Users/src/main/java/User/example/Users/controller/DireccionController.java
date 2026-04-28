package User.example.Users.controller;

import User.example.Users.model.DireccionModel;
import User.example.Users.repository.DireccionRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direcciones")
@Tag(name = "Direcciones", description = "Consulta de direcciones disponibles para asignar a usuarios")
public class DireccionController {

    @Autowired
    private DireccionRepository direccionRepository;

    @Operation(summary = "Listar todas las direcciones")
    @GetMapping
    public List<DireccionModel> getAll() {
        return direccionRepository.findAll();
    }

    @Operation(summary = "Obtener dirección por ID")
    @GetMapping("/{id}")
    public ResponseEntity<DireccionModel> getById(@PathVariable UUID id) {
        return direccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nueva dirección")
    @PostMapping
    public DireccionModel create(@RequestBody DireccionModel direccion) {
        return direccionRepository.save(direccion);
    }
}
