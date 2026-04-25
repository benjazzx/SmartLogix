package User.example.Users.controller;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.dto.UserResponseDto;
import User.example.Users.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('admin')")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema SmartLogix — solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Listar todos los usuarios")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(description = "UUID del usuario") @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(UserResponseDto.from(userService.getUserById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Buscar usuario por correo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/correo/{correo}")
    public ResponseEntity<UserResponseDto> getUserByCorreo(
            @Parameter(description = "Correo electrónico del usuario") @PathVariable String correo) {
        try {
            return ResponseEntity.ok(UserResponseDto.from(userService.getUserByCorreo(correo)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Listar usuarios por rol")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/por-rol/{rolId}")
    public List<UserResponseDto> getUsersByRol(
            @Parameter(description = "UUID del rol") @PathVariable UUID rolId) {
        return userService.getUsersByRol(rolId).stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Listar usuarios por estado")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/por-estado/{estadoId}")
    public List<UserResponseDto> getUsersByEstado(
            @Parameter(description = "UUID del estado") @PathVariable UUID estadoId) {
        return userService.getUsersByEstado(estadoId).stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Crear nuevo usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Correo o RUT duplicado")
    })
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequestDto dto) {
        try {
            return ResponseEntity.ok(UserResponseDto.from(userService.createUser(dto)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Actualizar usuario existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable UUID id, @RequestBody UserRequestDto dto) {
        try {
            return ResponseEntity.ok(UserResponseDto.from(userService.updateUser(id, dto)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Eliminar usuario por ID")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Asignar rol a un usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol asignado correctamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/{id}/asignar-rol")
    public ResponseEntity<UserResponseDto> asignarRol(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        try {
            UUID rolId = UUID.fromString(body.get("rolId"));
            String rolNombre = body.get("rolNombre");
            return ResponseEntity.ok(UserResponseDto.from(userService.asignarRol(id, rolId, rolNombre)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
