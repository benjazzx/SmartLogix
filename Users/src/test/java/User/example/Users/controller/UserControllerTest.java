package User.example.Users.controller;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.UserModel;
import User.example.Users.service.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void getAllUsers_debeRetornarLista() throws Exception {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setNombre("Juan");
        user.setCorreo("juan@test.cl");

        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].nombre").value("Juan"));
    }

    @Test
    void getUserById_existente_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(id);
        user.setNombre("María");

        when(userService.getUserById(id)).thenReturn(user);

        mockMvc.perform(get("/api/users/" + id))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("María"));
    }

    @Test
    void getUserById_noExistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserById(id)).thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/api/users/" + id))
               .andExpect(status().isNotFound());
    }

    @Test
    void createUser_valido_debeRetornar200() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setNombre("Carlos");
        dto.setApellido("González");
        dto.setRut("22222222-2");
        dto.setCorreo("carlos@test.cl");
        dto.setClave("pass123");
        dto.setRolNombre("transportista");

        UserModel saved = new UserModel();
        saved.setId(UUID.randomUUID());
        saved.setNombre("Carlos");

        when(userService.createUser(any())).thenReturn(saved);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    void createUser_correoExistente_debeRetornar400() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setCorreo("duplicado@test.cl");

        when(userService.createUser(any())).thenThrow(new RuntimeException("correo duplicado"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_existente_debeRetornar204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userService).deleteUser(id);

        mockMvc.perform(delete("/api/users/" + id))
               .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_noExistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("no encontrado")).when(userService).deleteUser(id);

        mockMvc.perform(delete("/api/users/" + id))
               .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_existente_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserRequestDto dto = new UserRequestDto();
        dto.setNombre("Nuevo Nombre");

        UserModel updated = new UserModel();
        updated.setId(id);
        updated.setNombre("Nuevo Nombre");

        when(userService.updateUser(eq(id), any())).thenReturn(updated);

        mockMvc.perform(put("/api/users/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Nuevo Nombre"));
    }

    @Test
    void updateUser_noExistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        UserRequestDto dto = new UserRequestDto();
        dto.setNombre("NoExiste");

        when(userService.updateUser(eq(id), any())).thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(put("/api/users/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
               .andExpect(status().isNotFound());
    }

    @Test
    void getUserByCorreo_existente_debeRetornar200() throws Exception {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo("test@smart.cl");
        user.setNombre("Ana");

        when(userService.getUserByCorreo("test@smart.cl")).thenReturn(user);

        mockMvc.perform(get("/api/users/correo/test@smart.cl"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void getUserByCorreo_noExistente_debeRetornar404() throws Exception {
        when(userService.getUserByCorreo("nope@smart.cl"))
                .thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(get("/api/users/correo/nope@smart.cl"))
               .andExpect(status().isNotFound());
    }

    @Test
    void getUsersByRol_retornaLista() throws Exception {
        UUID rolId = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setNombre("Bodeguero");

        when(userService.getUsersByRol(rolId)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users/por-rol/" + rolId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].nombre").value("Bodeguero"));
    }

    @Test
    void getUsersByEstado_retornaLista() throws Exception {
        UUID estadoId = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setNombre("Activo");

        when(userService.getUsersByEstado(estadoId)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users/por-estado/" + estadoId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].nombre").value("Activo"));
    }

    @Test
    void toggleActivo_existente_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserModel user = new UserModel();
        user.setId(id);
        user.setNombre("Pedro");
        user.setActivo(false);

        when(userService.toggleActivo(id)).thenReturn(user);

        mockMvc.perform(patch("/api/users/" + id + "/toggle-activo"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.nombre").value("Pedro"));
    }

    @Test
    void toggleActivo_noExistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.toggleActivo(id)).thenThrow(new RuntimeException("no encontrado"));

        mockMvc.perform(patch("/api/users/" + id + "/toggle-activo"))
               .andExpect(status().isNotFound());
    }

    @Test
    void asignarRol_valido_debeRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();

        UserModel user = new UserModel();
        user.setId(id);
        user.setNombre("Luis");
        user.setRolId(rolId);
        user.setRolNombre("bodeguero");

        when(userService.asignarRol(id, rolId, "bodeguero")).thenReturn(user);

        Map<String, String> body = Map.of("rolId", rolId.toString(), "rolNombre", "bodeguero");

        mockMvc.perform(post("/api/users/" + id + "/asignar-rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.rolNombre").value("bodeguero"));
    }

    @Test
    void asignarRol_usuarioNoExistente_debeRetornar404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();

        when(userService.asignarRol(any(), any(), any()))
                .thenThrow(new RuntimeException("usuario no encontrado"));

        Map<String, String> body = Map.of("rolId", rolId.toString(), "rolNombre", "admin");

        mockMvc.perform(post("/api/users/" + id + "/asignar-rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isNotFound());
    }
}
