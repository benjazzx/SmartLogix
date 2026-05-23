package User.example.Users.controller;

import User.example.Users.dto.DireccionRequestDto;
import User.example.Users.model.ComunaModel;
import User.example.Users.model.DireccionModel;
import User.example.Users.model.RegionModel;
import User.example.Users.repository.ComunaRepository;
import User.example.Users.repository.DireccionRepository;
import User.example.Users.repository.RegionRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Geo", description = "Regiones, comunas y direcciones")
public class DireccionController {

    private final DireccionRepository direccionRepository;
    private final ComunaRepository comunaRepository;
    private final RegionRepository regionRepository;

    // ─── Regiones ────────────────────────────────────────────────────────────

    @Operation(summary = "Listar todas las regiones")
    @GetMapping("/api/regiones")
    public List<RegionModel> getAllRegiones() {
        return regionRepository.findAll();
    }

    // ─── Comunas ─────────────────────────────────────────────────────────────

    @Operation(summary = "Listar comunas de una región")
    @GetMapping("/api/comunas/por-region/{regionId}")
    public List<ComunaModel> getComunasByRegion(@PathVariable UUID regionId) {
        return comunaRepository.findByRegion_Id(regionId);
    }

    // ─── Direcciones ──────────────────────────────────────────────────────────

    @Operation(summary = "Listar todas las direcciones")
    @GetMapping("/api/direcciones")
    public List<DireccionModel> getAll() {
        return direccionRepository.findAll();
    }

    @Operation(summary = "Obtener dirección por ID")
    @GetMapping("/api/direcciones/{id}")
    public ResponseEntity<DireccionModel> getById(@PathVariable UUID id) {
        return direccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nueva dirección")
    @PostMapping("/api/direcciones")
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
