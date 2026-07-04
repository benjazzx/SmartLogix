package Producto.example.Producto.controller;

import Producto.example.Producto.dto.HistorialStockResponseDTO;
import Producto.example.Producto.model.HistorialStockModel;
import Producto.example.Producto.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/historial-stock")
@RequiredArgsConstructor
@Tag(name = "Historial de Auditoría", description = "Historial global de cambios sobre productos — solo admin/bodeguero")
public class HistorialStockController {

    private final ProductoService productoService;

    @GetMapping
    @Operation(summary = "Listar historial de stock con filtros y paginación")
    public ResponseEntity<Page<HistorialStockResponseDTO>> getHistorialGlobal(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID bodegueroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<HistorialStockModel> historial = productoService.getHistorialGlobal(productoId, bodegueroId, desde, hasta, pageable);
        boolean isAdmin = esAdmin();
        return ResponseEntity.ok(historial.map(h -> HistorialStockResponseDTO.from(h, isAdmin)));
    }

    @GetMapping("/reciente")
    @Operation(summary = "Últimos cambios de stock/estado sobre cualquier producto, para el panel de auditoría")
    public ResponseEntity<List<HistorialStockResponseDTO>> getHistorialReciente(
            @RequestParam(defaultValue = "50") Integer limite,
            @RequestParam(required = false) UUID modificadoPorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        boolean isAdmin = esAdmin();
        return ResponseEntity.ok(productoService.getHistorialReciente(limite, modificadoPorId, desde, hasta).stream()
                .map(h -> HistorialStockResponseDTO.from(h, isAdmin))
                .toList());
    }

    private boolean esAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_admin"));
    }
}
