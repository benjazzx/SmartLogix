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
}
