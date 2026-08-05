package User.example.Users.controller;

import User.example.Users.model.PreguntaSeguridadModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class PreguntaSeguridadController {

    private static final String MENSAJE = "mensaje";
    private static final String ERROR   = "error";

    private final PreguntaSeguridadRepository preguntaRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/preguntas-seguridad/catalogo")
    public ResponseEntity<List<String>> getCatalogo() {
        return ResponseEntity.ok(List.of(
            "¿Cuál fue el nombre de tu mejor amigo/a de la infancia?",
            "¿Con quién te casaste o formaste pareja? (nombre completo)",
            "¿En qué ciudad naciste?",
            "¿Cuál es el nombre de tu primera mascota?",
            "¿Cuál fue el nombre de tu escuela primaria?",
            "¿Cuál es el apodo de tu madre?"
        ));
    }

    @PostMapping("/preguntas-seguridad")
    public ResponseEntity<Map<String, String>> guardarPreguntas(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody List<Map<String, String>> preguntas) {

        if (preguntas == null || preguntas.size() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR, "Se requieren al menos 3 preguntas"));
        }

        UUID uid = UUID.fromString(userId);
        preguntaRepo.deleteByUserId(uid);

        for (Map<String, String> p : preguntas) {
            String pregunta  = p.get("pregunta");
            String respuesta = p.get("respuesta");
            if (pregunta == null || respuesta == null || respuesta.isBlank()) continue;

            PreguntaSeguridadModel m = new PreguntaSeguridadModel();
            m.setUserId(uid);
            m.setPregunta(pregunta);
            m.setRespuesta(passwordEncoder.encode(respuesta.trim().toLowerCase()));
            preguntaRepo.save(m);
        }

        log.info("[Seguridad] Preguntas guardadas para userId: {}", userId);
        return ResponseEntity.ok(Map.of(MENSAJE, "Preguntas de seguridad guardadas"));
    }

    @GetMapping("/preguntas-seguridad/tiene/{userId}")
    public ResponseEntity<Map<String, Boolean>> tienePreguntas(@PathVariable String userId) {
        boolean tiene = preguntaRepo.existsByUserId(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("tiene", tiene));
    }

    @GetMapping("/preguntas-seguridad/usuario/{userId}")
    public ResponseEntity<Map<String, List<String>>> getPreguntasPorUsuario(
            @PathVariable String userId) {

        List<String> soloPreguntas = preguntaRepo.findByUserId(UUID.fromString(userId))
                .stream()
                .map(PreguntaSeguridadModel::getPregunta)
                .toList();
        return ResponseEntity.ok(Map.of("preguntas", soloPreguntas));
    }
}
