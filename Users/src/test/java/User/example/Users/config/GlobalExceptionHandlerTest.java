package User.example.Users.config;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_retorna404ConMensaje() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new EntityNotFoundException("recurso no encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("recurso no encontrado", response.getBody().get("mensaje"));
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void handleConflict_retorna409ConMensaje() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleConflict(new IllegalArgumentException("argumento invalido"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("argumento invalido", response.getBody().get("mensaje"));
    }

    @Test
    void handleRuntime_mensajeNoEncontrado_retorna404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("Usuario no encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleRuntime_mensajeYaExiste_retorna409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("Ya existe un usuario con ese correo"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleRuntime_mensajeGenerico_retorna400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("Error genérico"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleRuntime_mensajeNulo_retorna400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException((String) null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleGeneric_retorna500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new Exception("error interno"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().get("mensaje"));
    }

    @Test
    void handleRuntime_mensajeDuplicado_retorna409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("RUT duplicado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
