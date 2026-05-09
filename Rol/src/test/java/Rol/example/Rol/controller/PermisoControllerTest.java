package Rol.example.Rol.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.service.PermisoService;

@ExtendWith(MockitoExtension.class)
class PermisoControllerTest {

    @Mock
    private PermisoService permisoService;

    @InjectMocks
    private PermisoController permisoController;

    private MockMvc mockMvc;
    private PermisoModel permiso;
    private UUID rolId;
    private UUID privilegioId;
    private UUID permisoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permisoController).build();
        rolId = UUID.randomUUID();
        privilegioId = UUID.randomUUID();
        permisoId = UUID.randomUUID();
        TipoModel tipo = new TipoModel(UUID.randomUUID(), "OPERACION");
        RolModel rol = new RolModel(rolId, "bodeguero", "Encargado de bodega");
        PrivilegioModel privilegio = new PrivilegioModel(privilegioId, "VER_INVENTARIO", "Ver stock", tipo);
        permiso = new PermisoModel(permisoId, rol, privilegio);
    }

    @Test
    void testGetAllPermisos_retornaLista() throws Exception {
        when(permisoService.getAllPermisos()).thenReturn(List.of(permiso));

        mockMvc.perform(get("/api/permisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(permisoId.toString()));

        verify(permisoService).getAllPermisos();
    }

    @Test
    void testGetAllPermisos_listaVacia() throws Exception {
        when(permisoService.getAllPermisos()).thenReturn(List.of());

        mockMvc.perform(get("/api/permisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetPermisosByRol_rolExistente() throws Exception {
        when(permisoService.getPermisosByRol(rolId)).thenReturn(List.of(permiso));

        mockMvc.perform(get("/api/permisos/por-rol/{rolId}", rolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(permisoId.toString()));
    }

    @Test
    void testGetPermisosByRol_rolNoExiste() throws Exception {
        when(permisoService.getPermisosByRol(any(UUID.class))).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/permisos/por-rol/{rolId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testTienePermiso_retornaTrue() throws Exception {
        when(permisoService.tienePermiso(rolId, privilegioId)).thenReturn(true);

        mockMvc.perform(get("/api/permisos/verificar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testTienePermiso_retornaFalse() throws Exception {
        when(permisoService.tienePermiso(any(UUID.class), any(UUID.class))).thenReturn(false);

        mockMvc.perform(get("/api/permisos/verificar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void testAsignarPermiso_exitoso() throws Exception {
        when(permisoService.asignarPermiso(rolId, privilegioId)).thenReturn(permiso);

        mockMvc.perform(post("/api/permisos/asignar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isOk());

        verify(permisoService).asignarPermiso(rolId, privilegioId);
    }

    @Test
    void testAsignarPermiso_yaAsignado() throws Exception {
        when(permisoService.asignarPermiso(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Ya asignado"));

        mockMvc.perform(post("/api/permisos/asignar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRevocarPermiso_exitoso() throws Exception {
        doNothing().when(permisoService).revocarPermiso(rolId, privilegioId);

        mockMvc.perform(delete("/api/permisos/revocar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isNoContent());

        verify(permisoService).revocarPermiso(rolId, privilegioId);
    }

    @Test
    void testRevocarPermiso_noAsignado() throws Exception {
        doThrow(new RuntimeException("No asignado")).when(permisoService)
                .revocarPermiso(any(UUID.class), any(UUID.class));

        mockMvc.perform(delete("/api/permisos/revocar")
                        .param("rolId", rolId.toString())
                        .param("privilegioId", privilegioId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeletePermiso_exitoso() throws Exception {
        doNothing().when(permisoService).deletePermiso(permisoId);

        mockMvc.perform(delete("/api/permisos/{id}", permisoId))
                .andExpect(status().isNoContent());

        verify(permisoService).deletePermiso(permisoId);
    }

    @Test
    void testDeletePermiso_noEncontrado() throws Exception {
        doThrow(new RuntimeException("No encontrado")).when(permisoService).deletePermiso(any(UUID.class));

        mockMvc.perform(delete("/api/permisos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
