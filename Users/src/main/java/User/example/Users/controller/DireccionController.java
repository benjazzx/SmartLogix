package User.example.Users.controller;

import User.example.Users.dto.DireccionRequestDto;
import User.example.Users.model.ComunaModel;
import User.example.Users.model.DireccionModel;
import User.example.Users.repository.ComunaRepository;
import User.example.Users.repository.DireccionRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direcciones")
@RequiredArgsConstructor
@Tag(name = "Direcciones", description = "Consulta de direcciones disponibles para asignar a usuarios")
public class DireccionController {

    private final DireccionRepository direccionRepository;
    private final ComunaRepository comunaRepository;

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
    public DireccionModel create(@RequestBody DireccionRequestDto dto) {
        ComunaModel comuna = comunaRepository.findById(dto.getComunaId())
                .orElseThrow(() -> new RuntimeException("Comuna no encontrada: " + dto.getComunaId()));
        DireccionModel direccion = new DireccionModel();
        direccion.setCalle(dto.getCalle());
        direccion.setNumero(dto.getNumero());
        direccion.setCodigoPostal(dto.getCodigoPostal());
        direccion.setComuna(comuna);
        return direccionRepository.save(direccion);
    }
}
