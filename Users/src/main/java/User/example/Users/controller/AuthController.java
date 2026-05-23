package User.example.Users.controller;

import User.example.Users.client.RolClient;
import User.example.Users.dto.LoginRequestDto;
import User.example.Users.dto.LoginResponseDto;
import User.example.Users.dto.RegisterRequestDto;
import User.example.Users.dto.RolDto;
import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.UserModel;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Login y generación de token JWT")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private RolClient rolClient;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con correo y clave, retorna un token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso, token generado"),
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

            String token = jwtUtil.generateToken(user);

            return ResponseEntity.ok(new LoginResponseDto(
                token, "Bearer", user.getId(), user.getCorreo(), user.getRolNombre()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales inválidas");
        }
    }

    
    @Operation(summary = "Validar identidad para recuperar contraseña", description = "Valida que correo y RUT correspondan a un usuario")
    @PostMapping("/recuperar-clave")
    public ResponseEntity<?> recuperarClave(@RequestBody java.util.Map<String, String> body) {
        String correo = body.get("correo");
        String rut = body.get("rut");
        boolean valido = correo != null && rut != null
                && userRepository.findByCorreo(correo)
                        .map(u -> userService.hashRut(rut).equals(u.getRut()))
                        .orElse(false);
        if (valido) {
            log.info("[Auth] Identidad validada para cambio de clave: {}", correo);
        }
        return ResponseEntity.ok(java.util.Map.of("valido", valido));
    }

    @Operation(summary = "Cambiar contraseña", description = "Actualiza la contraseña tras validar correo y RUT")
    @PostMapping("/cambiar-clave")
    public ResponseEntity<?> cambiarClave(@RequestBody java.util.Map<String, String> body) {
        String correo    = body.get("correo");
        String rut       = body.get("rut");
        String nuevaClave = body.get("nuevaClave");
        if (correo == null || rut == null || nuevaClave == null || nuevaClave.length() < 6) {
            return ResponseEntity.badRequest().body("Datos incompletos o contraseña muy corta");
        }
        java.util.Optional<User.example.Users.model.UserModel> userOpt = userRepository.findByCorreo(correo);
        if (userOpt.isEmpty() || !userService.hashRut(rut).equals(userOpt.get().getRut())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        }
        User.example.Users.model.UserModel user = userOpt.get();
        user.setClave(passwordEncoder.encode(nuevaClave));
        userRepository.save(user);
        log.info("[Auth] Contraseña actualizada para: {}", correo);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Contraseña actualizada exitosamente"));
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

        // Resuelve rolId sincronamente para que no quede null en la respuesta
        RolDto rolCliente = rolClient.getRolByNombre("cliente");
        if (rolCliente != null) {
            userDto.setRolId(rolCliente.getId());
        }

        try {
            UserModel saved = userService.createUser(userDto);
            String token = jwtUtil.generateToken(saved);
            return ResponseEntity.ok(new LoginResponseDto(
                token, "Bearer", saved.getId(), saved.getCorreo(), saved.getRolNombre()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
