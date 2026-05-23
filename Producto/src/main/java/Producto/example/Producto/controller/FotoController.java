package Producto.example.Producto.controller;

import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.ProductoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Fotos", description = "Upload y consulta de imágenes de productos (almacenadas en BD)")
public class FotoController {

    private static final String PRODUCTO_NO_ENCONTRADO = "Producto no encontrado: ";

    private final ProductoRepository productoRepository;

    @PostMapping(value = "/api/productos/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o reemplazar imagen de un producto (admin / bodeguero)")
    public ResponseEntity<ProductoResponseDTO> subirFoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProductoModel producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTO_NO_ENCONTRADO + id));

        producto.setImagenData(file.getBytes());
        producto.setImagenTipo(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        productoRepository.save(producto);

        log.info("[FOTO] Guardada en BD para producto {}", id);
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }

    @GetMapping("/api/productos/{id}/foto")
    @Operation(summary = "Obtener imagen de un producto desde la BD (pública)")
    public ResponseEntity<byte[]> servirFoto(@PathVariable UUID id) {
        ProductoModel producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTO_NO_ENCONTRADO + id));

        if (producto.getImagenData() == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = producto.getImagenTipo() != null
                ? producto.getImagenTipo()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(producto.getImagenData());
    }

    @DeleteMapping("/api/productos/{id}/foto")
    @Operation(summary = "Eliminar imagen de un producto (admin / bodeguero)")
    public ResponseEntity<ProductoResponseDTO> eliminarFoto(@PathVariable UUID id) {
        ProductoModel producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PRODUCTO_NO_ENCONTRADO + id));

        producto.setImagenData(null);
        producto.setImagenTipo(null);
        productoRepository.save(producto);

        log.info("[FOTO] Eliminada de BD para producto {}", id);
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }
}
