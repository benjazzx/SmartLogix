package User.example.Users.controller;

import User.example.Users.client.RolClient;
import User.example.Users.dto.LoginRequestDto;
import User.example.Users.dto.LoginResponseDto;
import User.example.Users.dto.RegisterRequestDto;
import User.example.Users.dto.RolDto;
import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.PreguntaSeguridadModel;
import User.example.Users.model.UserModel;
import User.example.Users.repository.PreguntaSeguridadRepository;
import User.example.Users.repository.UserRepository;
import User.example.Users.security.JwtUtil;
import User.example.Users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Login y generación de token JWT")
public class AuthController {

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";
    private static final String BEARER = "Bearer";
    private static final Random RANDOM = new Random();

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private RolClient rolClient;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PreguntaSeguridadRepository preguntaRepo;

    @Operation(summary = "Iniciar sesión",
               description = "Paso 1: valida credenciales. Si el usuario tiene preguntas de seguridad " +
                             "configuradas devuelve status=CHALLENGE con un token de desafío (5 min). " +
                             "Si no, devuelve el JWT directamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso o desafío 2FA"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
        @ApiResponse(responseCode = "403", description = "Cuenta inactiva")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getClave())
            );

            UserModel user = userRepository.findByCorreo(request.getCorreo())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!user.getActivo()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Cuenta inactiva. Contacte al administrador.");
            }

            // 2FA: si el usuario tiene preguntas configuradas, emitir desafío
            List<PreguntaSeguridadModel> preguntas = preguntaRepo.findByUserId(user.getId());
            if (!preguntas.isEmpty()) {
                int idx = RANDOM.nextInt(preguntas.size());
                PreguntaSeguridadModel elegida = preguntas.get(idx);
                String challengeToken = jwtUtil.generateChallengeToken(user, elegida.getId());
                log.info("[Auth] 2FA challenge emitido para userId={}", user.getId());
                return ResponseEntity.ok(Map.of(
                    "status", "CHALLENGE",
                    "challengeToken", challengeToken,
                    "pregunta", elegida.getPregunta()
                ));
            }

            // Sin preguntas: flujo directo
            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new LoginResponseDto(
                token, BEARER,user.getId(), user.getCorreo(), user.getRolNombre()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CREDENCIALES_INVALIDAS);
        }
    }

    @Operation(summary = "Verificar pregunta de seguridad (paso 2 del login 2FA)",
               description = "Recibe el challengeToken del paso 1 y la respuesta a la pregunta. " +
                             "Si es correcta devuelve el JWT completo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Respuesta correcta, JWT emitido"),
        @ApiResponse(responseCode = "401", description = "Respuesta incorrecta o token expirado")
    })
    @PostMapping("/verificar-pregunta")
    public ResponseEntity<?> verificarPregunta(@RequestBody Map<String, String> body) {
        String challengeToken = body.get("challengeToken");
        String respuesta = body.get("respuesta");

        if (challengeToken == null || respuesta == null) {
            return ResponseEntity.badRequest().body("challengeToken y respuesta son requeridos");
        }
        if (!jwtUtil.isChallengeToken(challengeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token de desafío inválido o expirado");
        }

        Long preguntaId = jwtUtil.extractPreguntaId(challengeToken);
        String userId   = jwtUtil.extractUserIdClaim(challengeToken);

        return preguntaRepo.findById(preguntaId).map(pregunta -> {
            boolean correcta = passwordEncoder.matches(
                    respuesta.trim().toLowerCase(), pregunta.getRespuesta());
            if (!correcta) {
                log.warn("[Auth] Respuesta 2FA incorrecta para userId={}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body((Object) CREDENCIALES_INVALIDAS);
            }

            UserModel user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            String token = jwtUtil.generateToken(user);
            log.info("[Auth] 2FA completado, JWT emitido para userId={}", userId);
            return ResponseEntity.ok((Object) new LoginResponseDto(
                    token, BEARER,user.getId(), user.getCorreo(), user.getRolNombre()));
        }).orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Pregunta no encontrada"));
    }

    @Operation(summary = "Registrarse como cliente",
               description = "Endpoint público. Crea una cuenta con rol cliente y devuelve el token JWT.")
    @ApiResponse(responseCode = "200", description = "Registro exitoso, token generado")
    @ApiResponse(responseCode = "400", description = "Correo o RUT ya registrado / datos inválidos")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto dto) {
        if (userRepository.existsByCorreo(dto.getCorreo())) {
            return ResponseEntity.badRequest().body("Ya existe una cuenta con ese correo");
        }

        UserRequestDto userDto = new UserRequestDto();
        userDto.setNombre(dto.getNombre());
        userDto.setApellido(dto.getApellido());
        userDto.setRut(dto.getRut());
        userDto.setCorreo(dto.getCorreo());
        userDto.setClave(dto.getClave());
        userDto.setRolNombre("cliente");
        userDto.setDireccionId(dto.getDireccionId());

        RolDto rolCliente = rolClient.getRolByNombre("cliente");
        if (rolCliente != null) {
            userDto.setRolId(rolCliente.getId());
        }

        try {
            UserModel saved = userService.createUser(userDto);
            String token = jwtUtil.generateToken(saved);
            return ResponseEntity.ok(new LoginResponseDto(
                token, BEARER,saved.getId(), saved.getCorreo(), saved.getRolNombre()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
