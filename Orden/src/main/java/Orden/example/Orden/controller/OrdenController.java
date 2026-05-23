package Orden.example.Orden.controller;

import Orden.example.Orden.dto.OrdenRequestDto;
import Orden.example.Orden.dto.OrdenResponseDto;
import Orden.example.Orden.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ordenes")
@Tag(name = "Ordenes", description = "Gestión de órdenes en SmartLogix")
@SecurityRequirement(name = "bearerAuth")
public class OrdenController {

    private static final String ATTR_USER_ID = "userId";
    private static final String KEY_ERROR    = "error";

    @Autowired
    private OrdenService ordenService;

    @Operation(summary = "Crear nueva orden (solo cliente)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Object> createOrden(
            @Valid @RequestBody OrdenRequestDto dto,
            HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(KEY_ERROR, "Token inválido o expirado"));
        }
        try {
            return ResponseEntity.ok(ordenService.createOrden(dto, userId));
        } catch (RuntimeException e) {
            log.error("Error al crear orden para userId={}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "Ver mis órdenes (solo cliente)")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/mis-ordenes")
    public ResponseEntity<List<OrdenResponseDto>> getMisOrdenes(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ordenService.getMisOrdenes(userId));
    }

    @Operation(summary = "Listar todas las órdenes (admin, bodeguero, transportista)")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public List<OrdenResponseDto> getAll(HttpServletRequest request, Authentication auth) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        return ordenService.getAll(extractRol(auth), userId);
    }

    @Operation(summary = "Obtener orden por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponseDto> getById(
            @Parameter(description = "ID de la orden") @PathVariable Long id,
            HttpServletRequest request,
            Authentication auth) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        try {
            return ResponseEntity.ok(ordenService.getById(id, userId, extractRol(auth)));
        } catch (RuntimeException e) {
            log.warn("getById id={}: {}", id, e.getMessage());
            if (e.getMessage().contains("Acceso denegado")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Tomar orden como ruta (solo transportista)")
    @PostMapping("/{id}/tomar")
    public ResponseEntity<Object> tomarOrden(@PathVariable Long id, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of(KEY_ERROR, "Token inválido"));
        try {
            return ResponseEntity.ok(ordenService.tomarOrden(id, userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "Liberar orden (solo el transportista que la tomó)")
    @PostMapping("/{id}/liberar")
    public ResponseEntity<Object> liberarOrden(@PathVariable Long id, HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(ATTR_USER_ID);
        if (userId == null) return ResponseEntity.status(401).body(Map.of(KEY_ERROR, "Token inválido"));
        try {
            return ResponseEntity.ok(ordenService.liberarOrden(id, userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    private String extractRol(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) return "";
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
