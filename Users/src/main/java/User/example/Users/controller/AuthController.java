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

    
    @Operation(summary = "Recuperar contraseña", description = "Envía instrucciones si correo y RUT coinciden")
    @PostMapping("/recuperar-clave")
    public ResponseEntity<?> recuperarClave(@RequestBody java.util.Map<String, String> body) {
        String correo = body.get("correo");
        String rut = body.get("rut");
        boolean coincide = correo != null && rut != null
                && userRepository.findByCorreo(correo)
                        .map(u -> rut.equalsIgnoreCase(u.getRut()))
                        .orElse(false);
        // Log de auditoría; la respuesta es siempre genérica para evitar enumeración de usuarios
        if (coincide) {
            log.info("[Auth] Recuperación solicitada para correo válido: {}", correo);
        }
        return ResponseEntity.ok(java.util.Map.of(
            "mensaje", "Si el correo y RUT coinciden, recibirás instrucciones para recuperar tu contraseña."
        ));
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
