package Orden.example.Orden.controller;

import Orden.example.Orden.dto.HistorialRequestDto;
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
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ordenes/{ordenId}/historial")
@Tag(name = "Historial de Orden", description = "Gestión del historial de estados de una orden")
@SecurityRequirement(name = "bearerAuth")
public class HistorialController {

    @Autowired
    private OrdenService ordenService;

    @Operation(summary = "Agregar cambio de estado a una orden (admin, bodeguero, transportista)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado agregado correctamente"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @PostMapping
    public ResponseEntity<OrdenResponseDto> addHistorial(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            @Valid @RequestBody HistorialRequestDto dto,
            HttpServletRequest request,
            Authentication auth) {
        UUID userId = (UUID) request.getAttribute("userId");
        String rolNombre = (auth != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "";
        try {
            return ResponseEntity.ok(ordenService.addHistorial(ordenId, dto, userId, rolNombre));
        } catch (RuntimeException e) {
            log.warn("addHistorial ordenId={}: {}", ordenId, e.getMessage());
            if (e.getMessage().contains("Acceso denegado")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Ver historial de una orden")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    })
    @GetMapping
    public ResponseEntity<List<OrdenResponseDto.HistorialDto>> getHistorial(
            @Parameter(description = "ID de la orden") @PathVariable Long ordenId,
            HttpServletRequest request,
            Authentication auth) {
        UUID userId = (UUID) request.getAttribute("userId");
        String rolNombre = (auth != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "";
        try {
            return ResponseEntity.ok(ordenService.getHistorial(ordenId, userId, rolNombre));
        } catch (RuntimeException e) {
            log.warn("getHistorial ordenId={}: {}", ordenId, e.getMessage());
            if (e.getMessage().contains("Acceso denegado")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
}
