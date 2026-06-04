package User.example.Users.controller;

import User.example.Users.model.PreguntaSeguridadModel;
import User.example.Users.model.SolicitudRecuperacionModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
import User.example.Users.repository.SolicitudRecuperacionRepository;
import User.example.Users.repository.UserRepository;
import User.example.Users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final SolicitudRecuperacionRepository solicitudRepo;
    private final UserRepository userRepo;
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

    @PostMapping("/auth/solicitar-recuperacion")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(
            @RequestBody Map<String, String> body) {

        String correo = body.get("correo");
        if (correo == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Correo requerido"));
        }

        return userRepo.findByCorreo(correo).map(user -> {
            if (!preguntaRepo.existsByUserId(user.getId())) {
                return ResponseEntity.badRequest().<Map<String, String>>body(Map.of(
                    ERROR, "Este usuario no tiene preguntas de seguridad. Contacte al administrador."));
            }
            SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
            sol.setUserId(user.getId());
            sol.setCorreo(correo);
            sol.setNombreUsuario(user.getNombre() + " " + user.getApellido());
            solicitudRepo.save(sol);
            log.info("[Recuperacion] Solicitud creada para: {}", correo);
            return ResponseEntity.ok(Map.of(MENSAJE, "Solicitud enviada. El administrador revisará tu caso."));
        }).orElse(ResponseEntity.ok(Map.of(MENSAJE, "Si el correo existe, se ha enviado la solicitud.")));
    }

    @GetMapping("/auth/solicitudes-recuperacion")
    public ResponseEntity<List<SolicitudRecuperacionModel>> listarSolicitudes(
            @RequestParam(required = false) String estado) {

        List<SolicitudRecuperacionModel> resultado = (estado != null)
                ? solicitudRepo.findByEstadoOrderByFechaSolicitudDesc(estado.toUpperCase())
                : solicitudRepo.findAllByOrderByFechaSolicitudDesc();
        return ResponseEntity.ok(resultado);
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

    @PostMapping("/auth/solicitudes-recuperacion/{solicitudId}/resolver")
    public ResponseEntity<Map<String, String>> resolverSolicitud(
            @PathVariable Long solicitudId,
            @RequestBody Map<String, Object> body) {

        return solicitudRepo.findById(solicitudId).map(sol -> {
            String accion = (String) body.get("accion");

            if ("RECHAZAR".equalsIgnoreCase(accion)) {
                sol.setEstado("RECHAZADA");
                sol.setFechaResolucion(LocalDateTime.now());
                solicitudRepo.save(sol);
                return ResponseEntity.ok(Map.of(MENSAJE, "Solicitud rechazada"));
            }

            if ("APROBAR".equalsIgnoreCase(accion)) {
                sol.setEstado("APROBADA");
                sol.setFechaResolucion(LocalDateTime.now());
                solicitudRepo.save(sol);
                log.info("[Recuperacion] Solicitud aprobada para userId: {}", sol.getUserId());
                return ResponseEntity.ok(Map.of(MENSAJE, "Solicitud aprobada. El usuario puede cambiar su contraseña."));
            }

            return ResponseEntity.badRequest().<Map<String, String>>body(Map.of(ERROR, "Acción inválida"));
        }).orElse(ResponseEntity.notFound().<Map<String, String>>build());
    }

    @PostMapping("/auth/cambiar-clave-recuperacion")
    public ResponseEntity<Map<String, String>> cambiarClaveRecuperacion(
            @RequestBody Map<String, String> body) {

        String correo     = body.get("correo");
        String rut        = body.get("rut");
        String nuevaClave = body.get("nuevaClave");

        if (correo == null || rut == null || nuevaClave == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "correo, rut y nuevaClave son requeridos"));
        }
        try {
            UserService.validarContraseña(nuevaClave);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, e.getMessage()));
        }

        return userRepo.findByCorreo(correo).map(user -> {
            String rutNormalizado  = rut.replaceAll("[.\\s]", "").toUpperCase();
            String rutUsuario      = user.getRut() == null ? "" : user.getRut().replaceAll("[.\\s]", "").toUpperCase();
            if (!rutNormalizado.equals(rutUsuario)) {
                return ResponseEntity.badRequest()
                        .<Map<String, String>>body(Map.of(ERROR, "El RUT no coincide con el registrado."));
            }

            boolean tieneAprobada = solicitudRepo.findByUserIdOrderByFechaSolicitudDesc(user.getId())
                    .stream().anyMatch(s -> "APROBADA".equals(s.getEstado()));

            if (!tieneAprobada) {
                return ResponseEntity.badRequest()
                        .<Map<String, String>>body(Map.of(ERROR,
                            "No tienes una solicitud de recuperación aprobada. Espera la aprobación del administrador."));
            }

            user.setClave(passwordEncoder.encode(nuevaClave));
            userRepo.save(user);

            solicitudRepo.findByUserIdOrderByFechaSolicitudDesc(user.getId())
                    .stream().filter(s -> "APROBADA".equals(s.getEstado()))
                    .forEach(s -> {
                        s.setEstado("COMPLETADA");
                        s.setFechaResolucion(LocalDateTime.now());
                        solicitudRepo.save(s);
                    });

            log.info("[Recuperacion] Contraseña cambiada para: {}", correo);
            return ResponseEntity.ok(Map.of(MENSAJE, "Contraseña actualizada correctamente"));
        }).orElse(ResponseEntity.badRequest().<Map<String, String>>body(
            Map.of(ERROR, "Correo no registrado en el sistema")));
    }

    @PostMapping("/auth/cambiar-clave")
    public ResponseEntity<Map<String, String>> cambiarClave(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {

        String claveActual = body.get("claveActual");
        String nuevaClave  = body.get("nuevaClave");

        if (claveActual == null || nuevaClave == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR, "claveActual y nuevaClave son requeridos"));
        }
        try {
            UserService.validarContraseña(nuevaClave);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, e.getMessage()));
        }

        return userRepo.findById(UUID.fromString(userId)).map(user -> {
            if (!passwordEncoder.matches(claveActual, user.getClave())) {
                return ResponseEntity.badRequest()
                        .<Map<String, String>>body(Map.of(ERROR, "Contraseña actual incorrecta"));
            }
            user.setClave(passwordEncoder.encode(nuevaClave));
            userRepo.save(user);
            log.info("[Auth] Contraseña cambiada para userId: {}", userId);
            return ResponseEntity.ok(Map.of(MENSAJE, "Contraseña actualizada correctamente"));
        }).orElse(ResponseEntity.notFound().<Map<String, String>>build());
    }
}
