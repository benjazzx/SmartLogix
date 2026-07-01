package Rol.example.Rol.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
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

import Rol.example.Rol.dto.TipoRequestDto;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.service.TipoService;

@ExtendWith(MockitoExtension.class)
class TipoControllerTest {

    @Mock
    private TipoService tipoService;

    @InjectMocks
    private TipoController tipoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TipoModel tipo;
    private UUID tipoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tipoController).build();
        objectMapper = new ObjectMapper();
        tipoId = UUID.randomUUID();
        tipo = new TipoModel(tipoId, "LECTURA");
    }

    @Test
    void testGetAllTipos_retornaLista() throws Exception {
        when(tipoService.getAllTipos()).thenReturn(List.of(tipo));

        mockMvc.perform(get("/api/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("LECTURA"));

        verify(tipoService).getAllTipos();
    }

    @Test
    void testGetAllTipos_listaVacia() throws Exception {
        when(tipoService.getAllTipos()).thenReturn(List.of());

        mockMvc.perform(get("/api/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetTipoById_encontrado() throws Exception {
        when(tipoService.getTipoById(tipoId)).thenReturn(tipo);

        mockMvc.perform(get("/api/tipos/{id}", tipoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("LECTURA"));
    }

    @Test
    void testGetTipoById_noEncontrado() throws Exception {
        when(tipoService.getTipoById(any(UUID.class))).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/tipos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTipo_exitoso() throws Exception {
        TipoRequestDto dto = new TipoRequestDto();
        dto.setNombre("LECTURA");

        when(tipoService.createTipo(any(TipoModel.class))).thenReturn(tipo);

        mockMvc.perform(post("/api/tipos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("LECTURA"));

        verify(tipoService).createTipo(any(TipoModel.class));
    }

    @Test
    void testCreateTipo_nombreDuplicado() throws Exception {
        TipoRequestDto dto = new TipoRequestDto();
        dto.setNombre("LECTURA");

        when(tipoService.createTipo(any(TipoModel.class))).thenThrow(new RuntimeException("Duplicado"));

        mockMvc.perform(post("/api/tipos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateTipo_encontrado() throws Exception {
        TipoRequestDto dto = new TipoRequestDto();
        dto.setNombre("ESCRITURA");

        TipoModel actualizado = new TipoModel(tipoId, "ESCRITURA");
        when(tipoService.updateTipo(eq(tipoId), any(TipoModel.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/tipos/{id}", tipoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ESCRITURA"));
    }

    @Test
    void testUpdateTipo_noEncontrado() throws Exception {
        TipoRequestDto dto = new TipoRequestDto();
        dto.setNombre("ESCRITURA");

        when(tipoService.updateTipo(any(UUID.class), any(TipoModel.class)))
                .thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(put("/api/tipos/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteTipo_exitoso() throws Exception {
        doNothing().when(tipoService).deleteTipo(tipoId);

        mockMvc.perform(delete("/api/tipos/{id}", tipoId))
                .andExpect(status().isNoContent());

        verify(tipoService).deleteTipo(tipoId);
    }

    @Test
    void testDeleteTipo_noEncontrado() throws Exception {
        doThrow(new RuntimeException("No encontrado")).when(tipoService).deleteTipo(any(UUID.class));

        mockMvc.perform(delete("/api/tipos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
