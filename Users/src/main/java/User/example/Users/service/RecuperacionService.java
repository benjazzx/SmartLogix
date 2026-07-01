package User.example.Users.service;

import User.example.Users.model.IntentoRecuperacionModel;
import User.example.Users.model.SolicitudRecuperacionModel;
import User.example.Users.model.SolicitudRecuperacionModel.ResultadoVerificacion;
import User.example.Users.model.UserModel;
import User.example.Users.repository.IntentoRecuperacionRepository;
import User.example.Users.repository.SolicitudRecuperacionRepository;
import User.example.Users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacionService {

    private static final int MAX_INTENTOS = 3;
    private static final int EXPIRACION_MINUTOS = 15;

    private final SolicitudRecuperacionRepository solicitudRepo;
    private final IntentoRecuperacionRepository intentoRepo;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID solicitarRecuperacion(String correo) {
        if (!userRepository.existsByCorreo(correo)) {
            // Respuesta ambigua para no revelar si el correo existe
            throw new IllegalArgumentException("No se pudo procesar la solicitud");
        }
        SolicitudRecuperacionModel solicitud = SolicitudRecuperacionModel.builder()
                .correo(correo)
                .fecha(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS))
                .build();
        SolicitudRecuperacionModel saved = solicitudRepo.save(solicitud);
        log.info("[Recuperacion] Solicitud creada — id={} correo={}", saved.getId(), correo);
        return saved.getId();
    }

    @Transactional
    public void verificarRecuperacion(UUID solicitudId, String correo, String rut, String ip) {
        SolicitudRecuperacionModel solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        validarSolicitudActiva(solicitud);

        boolean identidadValida = correo.equalsIgnoreCase(solicitud.getCorreo())
                && userRepository.findByCorreo(correo)
                        .map(u -> userService.hashRut(rut).equals(u.getRut()))
                        .orElse(false);

        registrarIntento(solicitud, identidadValida, ip);

        if (!identidadValida) {
            solicitud.setIntentosFallidos(solicitud.getIntentosFallidos() + 1);
            if (solicitud.getIntentosFallidos() >= MAX_INTENTOS) {
                solicitud.setBloqueado(true);
                solicitud.setResultadoVerificacion(ResultadoVerificacion.BLOQUEADO);
                log.warn("[Recuperacion] Solicitud BLOQUEADA tras {} intentos — id={}", MAX_INTENTOS, solicitudId);
            }
            solicitudRepo.save(solicitud);
            throw new IllegalStateException(
                    solicitud.getBloqueado() ? "Solicitud bloqueada por exceso de intentos" : "Credenciales inválidas");
        }

        solicitud.setResultadoVerificacion(ResultadoVerificacion.EXITOSO);
        solicitudRepo.save(solicitud);
        log.info("[Recuperacion] Identidad verificada — id={}", solicitudId);
    }

    @Transactional
    public void cambiarClave(UUID solicitudId, String nuevaClave) {
        SolicitudRecuperacionModel solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (solicitud.getResultadoVerificacion() != ResultadoVerificacion.EXITOSO) {
            throw new IllegalStateException("La solicitud no ha sido verificada exitosamente");
        }
        if (LocalDateTime.now().isAfter(solicitud.getFechaExpiracion())) {
            throw new IllegalStateException("La solicitud ha expirado");
        }

        UserModel user = userRepository.findByCorreo(solicitud.getCorreo())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setClave(passwordEncoder.encode(nuevaClave));
        userRepository.save(user);

        solicitud.setResultadoVerificacion(ResultadoVerificacion.UTILIZADA);
        solicitudRepo.save(solicitud);
        log.info("[Recuperacion] Clave actualizada — solicitudId={} correo={}", solicitudId, solicitud.getCorreo());
    }

    @Transactional(readOnly = true)
    public List<IntentoRecuperacionModel> getIntentos(UUID solicitudId) {
        SolicitudRecuperacionModel solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        return intentoRepo.findBySolicitudOrderByFechaDesc(solicitud);
    }

    private void validarSolicitudActiva(SolicitudRecuperacionModel solicitud) {
        if (solicitud.getBloqueado()) {
            throw new IllegalStateException("Solicitud bloqueada por exceso de intentos");
        }
        if (solicitud.getResultadoVerificacion() == ResultadoVerificacion.UTILIZADA) {
            throw new IllegalStateException("Esta solicitud ya fue utilizada");
        }
        if (LocalDateTime.now().isAfter(solicitud.getFechaExpiracion())) {
            throw new IllegalStateException("La solicitud ha expirado");
        }
    }

    private void registrarIntento(SolicitudRecuperacionModel solicitud, boolean correcta, String ip) {
        IntentoRecuperacionModel intento = IntentoRecuperacionModel.builder()
                .solicitud(solicitud)
                .fecha(LocalDateTime.now())
                .correcta(correcta)
                .ip(ip)
                .build();
        intentoRepo.save(intento);
    }
}
