package Estado.example.Estado.controller;

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

import Estado.example.Estado.Controller.TipoDeEstadoController;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Service.TipoDeEstadoService;

@ExtendWith(MockitoExtension.class)
public class TipoDeEstadoControllerTest {

    @Mock
    private TipoDeEstadoService tipoDeEstadoService;

    @InjectMocks
    private TipoDeEstadoController tipoDeEstadoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TipoDeEstadoModel tipoCuenta;
    private TipoDeEstadoModel tipoLaboral;
    private UUID tipoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tipoDeEstadoController).build();
        objectMapper = new ObjectMapper();
        tipoId = UUID.randomUUID();
        tipoCuenta  = new TipoDeEstadoModel(tipoId, "cuenta", "Estados de cuenta de usuario");
        tipoLaboral = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Estados laborales");
    }

    @Test
    void testGetAll_retornaLista() throws Exception {
        when(tipoDeEstadoService.getAll()).thenReturn(List.of(tipoCuenta, tipoLaboral));

        mockMvc.perform(get("/api/tipos-estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("cuenta"))
                .andExpect(jsonPath("$[1].nombre").value("laboral"));

        verify(tipoDeEstadoService, times(1)).getAll();
    }

    @Test
    void testGetAll_listaVacia() throws Exception {
        when(tipoDeEstadoService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/tipos-estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetById_encontrado() throws Exception {
        when(tipoDeEstadoService.getById(tipoId)).thenReturn(tipoCuenta);

        mockMvc.perform(get("/api/tipos-estado/{id}", tipoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("cuenta"))
                .andExpect(jsonPath("$.descripcion").value("Estados de cuenta de usuario"));
    }

    @Test
    void testGetById_noEncontrado() throws Exception {
        when(tipoDeEstadoService.getById(any(UUID.class))).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/tipos-estado/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreate_guardaYRetorna() throws Exception {
        when(tipoDeEstadoService.create(any(TipoDeEstadoModel.class))).thenReturn(tipoCuenta);

        mockMvc.perform(post("/api/tipos-estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tipoCuenta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("cuenta"));

        verify(tipoDeEstadoService, times(1)).create(any(TipoDeEstadoModel.class));
    }

    @Test
    void testUpdate_encontrado() throws Exception {
        TipoDeEstadoModel actualizado = new TipoDeEstadoModel(tipoId, "producto", "Estados de producto");
        when(tipoDeEstadoService.update(eq(tipoId), any(TipoDeEstadoModel.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/tipos-estado/{id}", tipoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("producto"));
    }

    @Test
    void testUpdate_noEncontrado() throws Exception {
        when(tipoDeEstadoService.update(any(UUID.class), any(TipoDeEstadoModel.class)))
                .thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(put("/api/tipos-estado/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tipoCuenta)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDelete_exitoso() throws Exception {
        doNothing().when(tipoDeEstadoService).delete(tipoId);

        mockMvc.perform(delete("/api/tipos-estado/{id}", tipoId))
                .andExpect(status().isNoContent());

        verify(tipoDeEstadoService, times(1)).delete(tipoId);
    }
}
