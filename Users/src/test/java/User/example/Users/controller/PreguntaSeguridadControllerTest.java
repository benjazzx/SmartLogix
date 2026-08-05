package User.example.Users.controller;

import User.example.Users.model.PreguntaSeguridadModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PreguntaSeguridadControllerTest {

    @Mock private PreguntaSeguridadRepository preguntaRepo;
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
}
