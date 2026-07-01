package Producto.example.Producto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FotoService {

    private final Path storageLocation;

    public FotoService(@Value("${foto.storage.path:/app/fotos}") String storagePath) throws IOException {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(storageLocation);
        log.info("[FOTO] Directorio de almacenamiento: {}", storageLocation);
    }

    public String guardar(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String extension = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        String filename = UUID.randomUUID() + extension;
        Path target = storageLocation.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("[FOTO] Guardada: {}", filename);
        return filename;
    }

    public Resource cargar(String filename) throws MalformedURLException {
        Path filePath = storageLocation.resolve(filename).normalize();
        if (!filePath.startsWith(storageLocation)) {
            throw new RuntimeException("Ruta no permitida: " + filename);
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new RuntimeException("Foto no encontrada: " + filename);
        }
        return resource;
    }

    public void eliminar(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = storageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("[FOTO] Eliminada: {}", filename);
        } catch (IOException e) {
            log.warn("[FOTO] No se pudo eliminar: {}", filename);
        }
    }
}
