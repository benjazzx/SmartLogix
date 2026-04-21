package Rol.example.Rol.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;

@ExtendWith(MockitoExtension.class)
public class RolControllerTest {

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolController rolController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RolModel rolModel;
    private UUID rolId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolController).build();
        objectMapper = new ObjectMapper();
        rolId = UUID.randomUUID();
        rolModel = new RolModel(rolId, "bodeguero", "Encargado de bodega");
    }

    @Test
    void testGetAllRoles_retornaLista() throws Exception {
        when(rolService.getAllRoles()).thenReturn(List.of(rolModel));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("bodeguero"));

        verify(rolService, times(1)).getAllRoles();
    }

    @Test
    void testGetAllRoles_listaVacia() throws Exception {
        when(rolService.getAllRoles()).thenReturn(List.of());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetRolById_encontrado() throws Exception {
        when(rolService.getRolById(rolId)).thenReturn(rolModel);

        mockMvc.perform(get("/api/roles/{id}", rolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("bodeguero"))
                .andExpect(jsonPath("$.descripcion").value("Encargado de bodega"));
    }

    @Test
    void testGetRolById_noEncontrado() throws Exception {
        when(rolService.getRolById(any(UUID.class)))
                .thenThrow(new RuntimeException("Rol no encontrado"));

        mockMvc.perform(get("/api/roles/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAssignRoleByEmail_bodeguero() throws Exception {
        when(rolService.assignRoleByEmail("juan@smartb.cl")).thenReturn(rolModel);

        mockMvc.perform(get("/api/roles/assign").param("email", "juan@smartb.cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("bodeguero"));
    }

    @Test
    void testFallbackAssignRole_retornaRolTemporal() {
        RolModel fallback = rolController
                .fallbackAssignRole("user@test.cl", new RuntimeException("falla"))
                .getBody();

        assertNotNull(fallback);
        assertEquals("cliente_temporal_por_falla", fallback.getNombre());
        assertNotNull(fallback.getId());
    }

    @Test
    void testCreateRol() throws Exception {
        when(rolService.createRol(any(RolModel.class))).thenReturn(rolModel);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolModel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("bodeguero"));

        verify(rolService, times(1)).createRol(any(RolModel.class));
    }

    @Test
    void testUpdateRol_encontrado() throws Exception {
        RolModel actualizado = new RolModel(rolId, "admin", "Administrador");
        when(rolService.updateRol(eq(rolId), any(RolModel.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/roles/{id}", rolId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("admin"));
    }

    @Test
    void testUpdateRol_noEncontrado() throws Exception {
        when(rolService.updateRol(any(UUID.class), any(RolModel.class)))
                .thenThrow(new RuntimeException("Rol no encontrado"));

        mockMvc.perform(put("/api/roles/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolModel)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteRol_exitoso() throws Exception {
        doNothing().when(rolService).deleteRol(rolId);

        mockMvc.perform(delete("/api/roles/{id}", rolId))
                .andExpect(status().isNoContent());

        verify(rolService, times(1)).deleteRol(rolId);
    }
}
