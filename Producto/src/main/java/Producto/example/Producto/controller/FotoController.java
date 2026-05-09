package Producto.example.Producto.controller;

import Producto.example.Producto.dto.ProductoResponseDTO;
import Producto.example.Producto.model.ProductoModel;
import Producto.example.Producto.repository.ProductoRepository;
import Producto.example.Producto.service.FotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Fotos", description = "Upload y consulta de imágenes de productos")
public class FotoController {

    private final FotoService fotoService;
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
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        if (producto.getImagenUrl() != null) {
            fotoService.eliminar(producto.getImagenUrl());
        }

        String filename = fotoService.guardar(file);
        producto.setImagenUrl(filename);
        productoRepository.save(producto);

        log.info("[FOTO] Asociada a producto {}: {}", id, filename);
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }

    @DeleteMapping("/api/productos/{id}/foto")
    @Operation(summary = "Eliminar imagen de un producto (admin / bodeguero)")
    public ResponseEntity<ProductoResponseDTO> eliminarFoto(@PathVariable UUID id) {
        ProductoModel producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        if (producto.getImagenUrl() != null) {
            fotoService.eliminar(producto.getImagenUrl());
            producto.setImagenUrl(null);
            productoRepository.save(producto);
        }

        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }

    @GetMapping("/api/productos/fotos/{filename:.+}")
    @Operation(summary = "Obtener imagen de un producto (pública)")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) throws MalformedURLException {
        Resource resource = fotoService.cargar(filename);

        String contentType;
        try {
            contentType = Files.probeContentType(Path.of(filename));
            if (contentType == null) contentType = "application/octet-stream";
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
