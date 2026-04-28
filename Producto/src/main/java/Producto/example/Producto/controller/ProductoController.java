package Producto.example.Producto.controller;

import Producto.example.Producto.dto.ProductoRequestDTO;
import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión del catálogo de productos SmartLogix")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @Operation(summary = "Listar productos activos")
    public ResponseEntity<List<ProductoResponseDTO>> getAll() {
        return ResponseEntity.ok(productoService.getAll());
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los productos (incluye inactivos) — solo admin")
    public ResponseEntity<List<ProductoResponseDTO>> getAllIncluirInactivos() {
        return ResponseEntity.ok(productoService.getAllIncluirInactivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID — usado por Inventario vía ProductoClient")
    public ResponseEntity<ProductoResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.getById(id));
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Listar productos por categoría")
    public ResponseEntity<List<ProductoResponseDTO>> getByCategoria(@PathVariable UUID categoriaId) {
        return ResponseEntity.ok(productoService.getByCategoria(categoriaId));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar productos por nombre")
    public ResponseEntity<List<ProductoResponseDTO>> buscar(@RequestParam String nombre) {
        return ResponseEntity.ok(productoService.buscarPorNombre(nombre));
    }

    @GetMapping("/bajo-stock")
    @Operation(summary = "Listar productos con stock igual o menor al umbral (default 10)")
    public ResponseEntity<List<ProductoResponseDTO>> getBajoStock(
            @RequestParam(defaultValue = "10") Integer umbral) {
        return ResponseEntity.ok(productoService.getBajoStock(umbral));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto")
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto completo")
    public ResponseEntity<ProductoResponseDTO> actualizar(@PathVariable UUID id,
                                                          @Valid @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Actualizar stock del producto — llamado por Inventario tras una orden")
    public ResponseEntity<ProductoResponseDTO> actualizarStock(@PathVariable UUID id,
                                                               @RequestBody Map<String, Integer> body) {
        Integer nuevoStock = body.get("stock");
        if (nuevoStock == null || nuevoStock < 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(productoService.actualizarStock(id, nuevoStock));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar producto (soft delete)")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
