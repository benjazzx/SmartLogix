package User.example.Users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import User.example.Users.dto.DireccionRequestDto;
import User.example.Users.model.ComunaModel;
import User.example.Users.model.DireccionModel;
import User.example.Users.model.RegionModel;
import User.example.Users.repository.ComunaRepository;
import User.example.Users.repository.DireccionRepository;

@ExtendWith(MockitoExtension.class)
class DireccionControllerTest {

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    @InjectMocks
    private DireccionController direccionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ComunaModel comuna;
    private DireccionModel direccion;
    private UUID comunaId;
    private UUID direccionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(direccionController).build();
        objectMapper = new ObjectMapper();

        comunaId = UUID.randomUUID();
        direccionId = UUID.randomUUID();

        RegionModel region = new RegionModel(UUID.randomUUID(), "Región Metropolitana");
        comuna = new ComunaModel(comunaId, "Providencia", region);
        direccion = new DireccionModel(direccionId, "Av. Providencia", "1234", "7500000", comuna);
    }

    @Test
    void testGetAll_retornaLista() throws Exception {
        when(direccionRepository.findAll()).thenReturn(List.of(direccion));

        mockMvc.perform(get("/api/direcciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].calle").value("Av. Providencia"))
                .andExpect(jsonPath("$[0].numero").value("1234"));

        verify(direccionRepository).findAll();
    }

    @Test
    void testGetAll_listaVacia() throws Exception {
        when(direccionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/direcciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetById_encontrado() throws Exception {
        when(direccionRepository.findById(direccionId)).thenReturn(Optional.of(direccion));

        mockMvc.perform(get("/api/direcciones/{id}", direccionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calle").value("Av. Providencia"))
                .andExpect(jsonPath("$.codigoPostal").value("7500000"));
    }

    @Test
    void testGetById_noEncontrado() throws Exception {
        when(direccionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/direcciones/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate_exitoso() throws Exception {
        DireccionRequestDto dto = new DireccionRequestDto();
        dto.setCalle("Calle Nueva");
        dto.setNumero("500");
        dto.setCodigoPostal("8320000");
        dto.setComunaId(comunaId);

        DireccionModel creada = new DireccionModel(UUID.randomUUID(), "Calle Nueva", "500", "8320000", comuna);

        when(comunaRepository.findById(comunaId)).thenReturn(Optional.of(comuna));
        when(direccionRepository.save(any(DireccionModel.class))).thenReturn(creada);

        mockMvc.perform(post("/api/direcciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calle").value("Calle Nueva"))
                .andExpect(jsonPath("$.numero").value("500"));

        verify(direccionRepository).save(any(DireccionModel.class));
    }

    @Test
    void testCreate_comunaNoExiste_lanzaExcepcion() throws Exception {
        DireccionRequestDto dto = new DireccionRequestDto();
        dto.setCalle("Calle X");
        dto.setNumero("1");
        dto.setComunaId(UUID.randomUUID());

        when(comunaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
            mockMvc.perform(post("/api/direcciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
        );
    }
}
