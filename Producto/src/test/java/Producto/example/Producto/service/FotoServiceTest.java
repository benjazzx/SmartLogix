package Producto.example.Producto.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FotoServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void guardarCargarYEliminar() throws Exception {
        FotoService servicio = new FotoService(carpeta.toString());

        String nombre = servicio.guardar(new MockMultipartFile("file", "caja.PNG", "image/png", new byte[] {1, 2}));
        assertTrue(nombre.endsWith(".png"));
        assertTrue(servicio.cargar(nombre).exists());

        servicio.eliminar(nombre);
        assertTrue(Files.notExists(carpeta.resolve(nombre)));
    }

    @Test
    void sinExtension_usaJpg() throws Exception {
        FotoService servicio = new FotoService(carpeta.toString());
        String nombre = servicio.guardar(new MockMultipartFile("file", "caja", "image/jpeg", new byte[] {1}));
        assertTrue(nombre.endsWith(".jpg"));
    }

    @Test
    void rechazaRutasFueraDeLaCarpetaYFotosInexistentes() throws Exception {
        FotoService servicio = new FotoService(carpeta.toString());
        assertThrows(RuntimeException.class, () -> servicio.cargar("../fuera.png"));
        assertThrows(RuntimeException.class, () -> servicio.cargar("no-existe.png"));
        assertDoesNotThrow(() -> servicio.eliminar(null));
        assertDoesNotThrow(() -> servicio.eliminar(" "));
    }
}
