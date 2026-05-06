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

import Rol.example.Rol.dto.PrivilegioRequestDto;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.service.PrivilegioService;

@ExtendWith(MockitoExtension.class)
class PrivilegioControllerTest {

    @Mock
    private PrivilegioService privilegioService;

    @InjectMocks
    private PrivilegioController privilegioController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PrivilegioModel privilegio;
    private TipoModel tipo;
    private UUID privilegioId;
    private UUID tipoId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(privilegioController).build();
        objectMapper = new ObjectMapper();
        tipoId = UUID.randomUUID();
        privilegioId = UUID.randomUUID();
        tipo = new TipoModel(tipoId, "OPERACION");
        privilegio = new PrivilegioModel(privilegioId, "VER_INVENTARIO", "Ver stock", tipo);
    }

    @Test
    void testGetAllPrivilegios_retornaLista() throws Exception {
        when(privilegioService.getAllPrivilegios()).thenReturn(List.of(privilegio));

        mockMvc.perform(get("/api/privilegios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("VER_INVENTARIO"));

        verify(privilegioService).getAllPrivilegios();
    }

    @Test
    void testGetAllPrivilegios_listaVacia() throws Exception {
        when(privilegioService.getAllPrivilegios()).thenReturn(List.of());

        mockMvc.perform(get("/api/privilegios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetPrivilegioById_encontrado() throws Exception {
        when(privilegioService.getPrivilegioById(privilegioId)).thenReturn(privilegio);

        mockMvc.perform(get("/api/privilegios/{id}", privilegioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("VER_INVENTARIO"))
                .andExpect(jsonPath("$.descripcion").value("Ver stock"));
    }

    @Test
    void testGetPrivilegioById_noEncontrado() throws Exception {
        when(privilegioService.getPrivilegioById(any(UUID.class))).thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(get("/api/privilegios/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPrivilegiosByTipo_encontrado() throws Exception {
        when(privilegioService.getPrivilegiosByTipo(tipoId)).thenReturn(List.of(privilegio));

        mockMvc.perform(get("/api/privilegios/por-tipo/{tipoId}", tipoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("VER_INVENTARIO"));
    }

    @Test
    void testGetPrivilegiosByTipo_tipoNoExiste() throws Exception {
        when(privilegioService.getPrivilegiosByTipo(any(UUID.class))).thenThrow(new RuntimeException("Tipo no encontrado"));

        mockMvc.perform(get("/api/privilegios/por-tipo/{tipoId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreatePrivilegio_exitoso() throws Exception {
        PrivilegioRequestDto dto = new PrivilegioRequestDto();
        dto.setNombre("VER_INVENTARIO");
        dto.setDescripcion("Ver stock");
        dto.setTipoId(tipoId);

        when(privilegioService.createPrivilegio(any(PrivilegioModel.class))).thenReturn(privilegio);

        mockMvc.perform(post("/api/privilegios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("VER_INVENTARIO"));

        verify(privilegioService).createPrivilegio(any(PrivilegioModel.class));
    }

    @Test
    void testCreatePrivilegio_duplicado() throws Exception {
        PrivilegioRequestDto dto = new PrivilegioRequestDto();
        dto.setNombre("VER_INVENTARIO");
        dto.setDescripcion("Ver stock");
        dto.setTipoId(tipoId);

        when(privilegioService.createPrivilegio(any(PrivilegioModel.class))).thenThrow(new RuntimeException("Duplicado"));

        mockMvc.perform(post("/api/privilegios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePrivilegio_encontrado() throws Exception {
        PrivilegioRequestDto dto = new PrivilegioRequestDto();
        dto.setNombre("EDITAR_INVENTARIO");
        dto.setDescripcion("Editar stock");
        dto.setTipoId(tipoId);

        PrivilegioModel actualizado = new PrivilegioModel(privilegioId, "EDITAR_INVENTARIO", "Editar stock", tipo);
        when(privilegioService.updatePrivilegio(eq(privilegioId), any(PrivilegioModel.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/privilegios/{id}", privilegioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("EDITAR_INVENTARIO"));
    }

    @Test
    void testUpdatePrivilegio_noEncontrado() throws Exception {
        PrivilegioRequestDto dto = new PrivilegioRequestDto();
        dto.setNombre("EDITAR_INVENTARIO");
        dto.setDescripcion("Editar stock");
        dto.setTipoId(tipoId);

        when(privilegioService.updatePrivilegio(any(UUID.class), any(PrivilegioModel.class)))
                .thenThrow(new RuntimeException("No encontrado"));

        mockMvc.perform(put("/api/privilegios/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeletePrivilegio_exitoso() throws Exception {
        doNothing().when(privilegioService).deletePrivilegio(privilegioId);

        mockMvc.perform(delete("/api/privilegios/{id}", privilegioId))
                .andExpect(status().isNoContent());

        verify(privilegioService).deletePrivilegio(privilegioId);
    }

    @Test
    void testDeletePrivilegio_noEncontrado() throws Exception {
        doThrow(new RuntimeException("No encontrado")).when(privilegioService).deletePrivilegio(any(UUID.class));

        mockMvc.perform(delete("/api/privilegios/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
