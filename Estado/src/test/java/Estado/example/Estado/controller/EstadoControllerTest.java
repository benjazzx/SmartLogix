package Estado.example.Estado.controller;

import static org.junit.jupiter.api.Assertions.*;
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

import Estado.example.Estado.Controller.EstadoController;
import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Service.EstadoService;

@ExtendWith(MockitoExtension.class)
public class EstadoControllerTest {

    @Mock
    private EstadoService estadoService;

    @InjectMocks
    private EstadoController estadoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Estado estadoActivo;
    private Estado estadoDisponible;
    private TipoDeEstadoModel tipoCuenta;
    private TipoDeEstadoModel tipoLaboral;
    private UUID estadoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(estadoController).build();
        objectMapper = new ObjectMapper();
        estadoId = UUID.randomUUID();
        tipoCuenta  = new TipoDeEstadoModel(UUID.randomUUID(), "cuenta", "Estados de cuenta");
        tipoLaboral = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Estados laborales");
        estadoActivo     = new Estado(estadoId, "activo", "Usuario activo", tipoCuenta);
        estadoDisponible = new Estado(UUID.randomUUID(), "disponible", "Empleado disponible", tipoLaboral);
    }

    @Test
    void testGetAll_retornaLista() throws Exception {
        when(estadoService.getAll()).thenReturn(List.of(estadoActivo, estadoDisponible));

        mockMvc.perform(get("/api/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("activo"))
                .andExpect(jsonPath("$[1].nombre").value("disponible"));

        verify(estadoService, times(1)).getAll();
    }

    @Test
    void testGetAll_listaVacia() throws Exception {
        when(estadoService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetById_encontrado() throws Exception {
        when(estadoService.getById(estadoId)).thenReturn(estadoActivo);

        mockMvc.perform(get("/api/estados/{id}", estadoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("activo"))
                .andExpect(jsonPath("$.descripcion").value("Usuario activo"));
    }

    @Test
    void testGetById_noEncontrado() throws Exception {
        when(estadoService.getById(any(UUID.class))).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/estados/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAssignEstadoByRol_bodeguero() throws Exception {
        when(estadoService.assignEstadoByRol("bodeguero")).thenReturn(estadoDisponible);

        mockMvc.perform(get("/api/estados/assign").param("roleName", "bodeguero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("disponible"));
    }

    @Test
    void testFallbackAssignEstado_retornaEstadoTemporal() {
        Estado fallback = estadoController
                .fallbackAssignEstado("cliente", new RuntimeException("falla"))
                .getBody();

        assertNotNull(fallback);
        assertEquals("pendiente_verificacion_temporal", fallback.getNombre());
        assertNotNull(fallback.getId());
    }

    @Test
    void testGetByTipo_retornaLista() throws Exception {
        when(estadoService.getByTipoNombre("laboral")).thenReturn(List.of(estadoDisponible));

        mockMvc.perform(get("/api/estados/tipo/laboral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("disponible"));
    }

    @Test
    void testCreate_guardaYRetorna() throws Exception {
        when(estadoService.create(any(Estado.class))).thenReturn(estadoActivo);

        mockMvc.perform(post("/api/estados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(estadoActivo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("activo"));

        verify(estadoService, times(1)).create(any(Estado.class));
    }

    @Test
    void testUpdate_encontrado() throws Exception {
        Estado actualizado = new Estado(estadoId, "inactivo", "Usuario inactivo", tipoCuenta);
        when(estadoService.update(eq(estadoId), any(Estado.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/estados/{id}", estadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("inactivo"));
    }

    @Test
    void testUpdate_noEncontrado() throws Exception {
        when(estadoService.update(any(UUID.class), any(Estado.class)))
                .thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(put("/api/estados/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(estadoActivo)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDelete_exitoso() throws Exception {
        doNothing().when(estadoService).delete(estadoId);

        mockMvc.perform(delete("/api/estados/{id}", estadoId))
                .andExpect(status().isNoContent());

        verify(estadoService, times(1)).delete(estadoId);
    }
}
