package User.example.Users.controller;

import User.example.Users.client.RolClient;
import User.example.Users.dto.LoginRequestDto;
import User.example.Users.dto.RolDto;
import User.example.Users.model.UserModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
import User.example.Users.repository.UserRepository;
import User.example.Users.security.JwtUtil;
import User.example.Users.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private RolClient rolClient;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private PreguntaSeguridadRepository preguntaRepo;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_credencialesValidas_retornaToken() throws Exception {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo("user@test.cl");
        user.setRolNombre("cliente");
        user.setActivo(true);

        when(userRepository.findByCorreo("user@test.cl")).thenReturn(Optional.of(user));
        when(preguntaRepo.findByUserId(user.getId())).thenReturn(java.util.List.of());
        when(jwtUtil.generateToken(user)).thenReturn("fake-jwt-token");

        LoginRequestDto req = new LoginRequestDto();
        req.setCorreo("user@test.cl");
        req.setClave("clave123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        LoginRequestDto req = new LoginRequestDto();
        req.setCorreo("bad@test.cl");
        req.setClave("wrong");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioInactivo_retorna403() throws Exception {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo("inactive@test.cl");
        user.setActivo(false);

        when(userRepository.findByCorreo("inactive@test.cl")).thenReturn(Optional.of(user));

        LoginRequestDto req = new LoginRequestDto();
        req.setCorreo("inactive@test.cl");
        req.setClave("clave123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_correoNuevo_retornaToken() throws Exception {
        UserModel saved = new UserModel();
        saved.setId(UUID.randomUUID());
        saved.setCorreo("nuevo@test.cl");
        saved.setRolNombre("cliente");

        RolDto rolDto = new RolDto(UUID.randomUUID(), "cliente", "Rol cliente");

        when(userRepository.existsByCorreo("nuevo@test.cl")).thenReturn(false);
        when(rolClient.getRolByNombre("cliente")).thenReturn(rolDto);
        when(userService.createUser(any())).thenReturn(saved);
        when(jwtUtil.generateToken(saved)).thenReturn("registro-token");

        String body = "{\"nombre\":\"Juan\",\"apellido\":\"Perez\",\"rut\":\"12345678-9\"," +
                "\"correo\":\"nuevo@test.cl\",\"clave\":\"clave123\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("registro-token"));
    }

    @Test
    void register_correoExistente_retorna400() throws Exception {
        when(userRepository.existsByCorreo("existe@test.cl")).thenReturn(true);

        String body = "{\"nombre\":\"Juan\",\"apellido\":\"Perez\",\"rut\":\"12345678-9\"," +
                "\"correo\":\"existe@test.cl\",\"clave\":\"clave123\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_servicioFalla_retorna400() throws Exception {
        when(userRepository.existsByCorreo(anyString())).thenReturn(false);
        when(rolClient.getRolByNombre("cliente")).thenReturn(null);
        when(userService.createUser(any())).thenThrow(new RuntimeException("DB error"));

        String body = "{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"rut\":\"98765432-1\"," +
                "\"correo\":\"error@test.cl\",\"clave\":\"clave123\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
