package User.example.Users.controller;

import User.example.Users.model.PreguntaSeguridadModel;
import User.example.Users.model.SolicitudRecuperacionModel;
import User.example.Users.model.UserModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
import User.example.Users.repository.SolicitudRecuperacionRepository;
import User.example.Users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PreguntaSeguridadControllerTest {

    @Mock private PreguntaSeguridadRepository preguntaRepo;
    @Mock private SolicitudRecuperacionRepository solicitudRepo;
    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private PreguntaSeguridadController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /preguntas-seguridad/catalogo ────────────────────────────────────

    @Test
    void getCatalogo_retornaSeisPreguntas() throws Exception {
        mockMvc.perform(get("/preguntas-seguridad/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    // ── POST /preguntas-seguridad ────────────────────────────────────────────

    @Test
    void guardarPreguntas_menosDeTres_retorna400() throws Exception {
        List<Map<String, String>> body = List.of(
            Map.of("pregunta", "Q1", "respuesta", "R1"),
            Map.of("pregunta", "Q2", "respuesta", "R2")
        );

        mockMvc.perform(post("/preguntas-seguridad")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guardarPreguntas_tresPreguntasValidas_retorna200() throws Exception {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        List<Map<String, String>> body = List.of(
            Map.of("pregunta", "Q1", "respuesta", "R1"),
            Map.of("pregunta", "Q2", "respuesta", "R2"),
            Map.of("pregunta", "Q3", "respuesta", "R3")
        );

        mockMvc.perform(post("/preguntas-seguridad")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(preguntaRepo, times(3)).save(any());
    }

    @Test
    void guardarPreguntas_respuestaBlanca_seIgnora() throws Exception {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        List<Map<String, String>> body = new ArrayList<>();
        Map<String, String> p1 = new LinkedHashMap<>();
        p1.put("pregunta", "Q1");
        p1.put("respuesta", "R1");
        Map<String, String> p2 = new LinkedHashMap<>();
        p2.put("pregunta", "Q2");
        p2.put("respuesta", "   ");  // blank → skip
        Map<String, String> p3 = new LinkedHashMap<>();
        p3.put("pregunta", "Q3");
        p3.put("respuesta", "R3");
        body.add(p1);
        body.add(p2);
        body.add(p3);

        mockMvc.perform(post("/preguntas-seguridad")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(preguntaRepo, times(2)).save(any()); // solo las 2 con respuesta
    }

    // ── GET /preguntas-seguridad/tiene/{userId} ──────────────────────────────

    @Test
    void tienePreguntas_conPreguntas_retornaTrue() throws Exception {
        UUID uid = UUID.randomUUID();
        when(preguntaRepo.existsByUserId(uid)).thenReturn(true);

        mockMvc.perform(get("/preguntas-seguridad/tiene/" + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiene").value(true));
    }

    @Test
    void tienePreguntas_sinPreguntas_retornaFalse() throws Exception {
        UUID uid = UUID.randomUUID();
        when(preguntaRepo.existsByUserId(uid)).thenReturn(false);

        mockMvc.perform(get("/preguntas-seguridad/tiene/" + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiene").value(false));
    }

    // ── POST /auth/solicitar-recuperacion ───────────────────────────────────

    @Test
    void solicitarRecuperacion_correoNoExiste_retornaOk() throws Exception {
        when(userRepo.findByCorreo("noexiste@test.cl")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/solicitar-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("correo", "noexiste@test.cl"))))
                .andExpect(status().isOk());
    }

    @Test
    void solicitarRecuperacion_correoExiste_retorna200() throws Exception {
        UUID uid = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(uid);
        user.setNombre("Ana");
        user.setApellido("Lopez");

        when(userRepo.findByCorreo("ana@test.cl")).thenReturn(Optional.of(user));
        when(solicitudRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/auth/solicitar-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("correo", "ana@test.cl"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void solicitarRecuperacion_sinCampoCorreo_retorna400() throws Exception {
        mockMvc.perform(post("/auth/solicitar-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    // ── GET /auth/solicitudes-recuperacion ──────────────────────────────────

    @Test
    void listarSolicitudes_sinFiltro_retornaLista() throws Exception {
        when(solicitudRepo.findAllByOrderByFechaSolicitudDesc()).thenReturn(List.of());

        mockMvc.perform(get("/auth/solicitudes-recuperacion"))
                .andExpect(status().isOk());
    }

    @Test
    void listarSolicitudes_conFiltroEstado_retornaLista() throws Exception {
        when(solicitudRepo.findByEstadoOrderByFechaSolicitudDesc("PENDIENTE")).thenReturn(List.of());

        mockMvc.perform(get("/auth/solicitudes-recuperacion").param("estado", "PENDIENTE"))
                .andExpect(status().isOk());
    }

    // ── GET /preguntas-seguridad/usuario/{userId} ────────────────────────────

    @Test
    void getPreguntasPorUsuario_retornaPreguntas() throws Exception {
        UUID uid = UUID.randomUUID();
        PreguntaSeguridadModel p = new PreguntaSeguridadModel();
        p.setPregunta("¿Tu primera mascota?");

        when(preguntaRepo.findByUserId(uid)).thenReturn(List.of(p));

        mockMvc.perform(get("/preguntas-seguridad/usuario/" + uid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntas[0]").value("¿Tu primera mascota?"));
    }

    // ── POST /auth/solicitudes-recuperacion/{id}/resolver ───────────────────

    @Test
    void resolverSolicitud_noEncontrada_retorna404() throws Exception {
        when(solicitudRepo.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/solicitudes-recuperacion/99/resolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("accion", "RECHAZAR"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolverSolicitud_rechazar_retorna200() throws Exception {
        SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
        sol.setId(1L);
        sol.setUserId(UUID.randomUUID());

        when(solicitudRepo.findById(1L)).thenReturn(Optional.of(sol));
        when(solicitudRepo.save(any())).thenReturn(sol);

        mockMvc.perform(post("/auth/solicitudes-recuperacion/1/resolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("accion", "RECHAZAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void resolverSolicitud_aprobar_retorna200() throws Exception {
        SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
        sol.setId(2L);
        sol.setUserId(UUID.randomUUID());

        when(solicitudRepo.findById(2L)).thenReturn(Optional.of(sol));
        when(solicitudRepo.save(any())).thenReturn(sol);

        mockMvc.perform(post("/auth/solicitudes-recuperacion/2/resolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("accion", "APROBAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void resolverSolicitud_rechazar_con_motivo_retorna200() throws Exception {
        SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
        sol.setId(3L);
        sol.setUserId(UUID.randomUUID());

        when(solicitudRepo.findById(3L)).thenReturn(Optional.of(sol));
        when(solicitudRepo.save(any())).thenReturn(sol);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accion", "RECHAZAR");
        body.put("motivo", "Identidad no verificada");

        mockMvc.perform(post("/auth/solicitudes-recuperacion/3/resolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void resolverSolicitud_accionInvalida_retorna400() throws Exception {
        SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
        sol.setId(4L);
        sol.setUserId(UUID.randomUUID());

        when(solicitudRepo.findById(4L)).thenReturn(Optional.of(sol));

        mockMvc.perform(post("/auth/solicitudes-recuperacion/4/resolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("accion", "DESCONOCIDA"))))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/cambiar-clave ─────────────────────────────────────────────

    @Test
    void cambiarClave_camposRequeridos_retorna400() throws Exception {
        mockMvc.perform(post("/auth/cambiar-clave")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("claveActual", "vieja"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClave_nuevaClaveInvalida_retorna400() throws Exception {
        mockMvc.perform(post("/auth/cambiar-clave")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "claveActual", "viejaPass",
                    "nuevaClave", "corta"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClave_claveActualIncorrecta_retorna400() throws Exception {
        UUID uid = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(uid);
        user.setClave("hashedVieja");

        when(userRepo.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("vieja123", "hashedVieja")).thenReturn(false);

        mockMvc.perform(post("/auth/cambiar-clave")
                .header("X-User-Id", uid.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "claveActual", "vieja123",
                    "nuevaClave", "NuevaClave123!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClave_exitosa_retorna200() throws Exception {
        UUID uid = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(uid);
        user.setClave("hashedVieja");

        when(userRepo.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("vieja123", "hashedVieja")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("hashedNueva");
        when(userRepo.save(any())).thenReturn(user);

        mockMvc.perform(post("/auth/cambiar-clave")
                .header("X-User-Id", uid.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "claveActual", "vieja123",
                    "nuevaClave", "NuevaClave123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    // ── POST /auth/cambiar-clave-recuperacion ────────────────────────────────

    @Test
    void cambiarClaveRecuperacion_camposFaltantes_retorna400() throws Exception {
        mockMvc.perform(post("/auth/cambiar-clave-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("correo", "test@test.cl"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClaveRecuperacion_correoNoExiste_retorna400() throws Exception {
        when(userRepo.findByCorreo("noexiste@test.cl")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/cambiar-clave-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "correo", "noexiste@test.cl",
                    "nuevaClave", "NuevaClave1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClaveRecuperacion_sinSolicitudAprobada_retorna400() throws Exception {
        UUID uid = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(uid);

        when(userRepo.findByCorreo("user@test.cl")).thenReturn(Optional.of(user));
        when(solicitudRepo.findByUserIdOrderByFechaSolicitudDesc(uid)).thenReturn(List.of());

        mockMvc.perform(post("/auth/cambiar-clave-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "correo", "user@test.cl",
                    "nuevaClave", "NuevaClave1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarClaveRecuperacion_exitosa_retorna200() throws Exception {
        UUID uid = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(uid);

        SolicitudRecuperacionModel sol = new SolicitudRecuperacionModel();
        sol.setId(10L);
        sol.setUserId(uid);
        sol.setEstado("APROBADA");

        when(userRepo.findByCorreo("user@test.cl")).thenReturn(Optional.of(user));
        when(solicitudRepo.findByUserIdOrderByFechaSolicitudDesc(uid)).thenReturn(List.of(sol));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNueva");
        when(userRepo.save(any())).thenReturn(user);
        when(solicitudRepo.save(any())).thenReturn(sol);

        mockMvc.perform(post("/auth/cambiar-clave-recuperacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "correo", "user@test.cl",
                    "nuevaClave", "NuevaClave1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }
}
