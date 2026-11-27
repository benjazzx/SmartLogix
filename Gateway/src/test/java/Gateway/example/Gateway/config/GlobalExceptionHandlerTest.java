package Gateway.example.Gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.AccessDeniedException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void rutaInexistente_responde404() {
        var r = handler.handleNotFound(new NoHandlerFoundException("GET", "/nada", new HttpHeaders()));
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    void accesoDenegado_responde403() {
        assertEquals(403, handler.handleAccessDenied(new AccessDeniedException("x")).getStatusCode().value());
    }

    @Test
    void argumentoInvalido_responde400() {
        assertEquals(400, handler.handleBadRequest(new IllegalArgumentException("dato")).getStatusCode().value());
    }

    @Test
    void tokenInvalido_responde401() {
        assertEquals(401, handler.handleRuntime(new RuntimeException("JWT expirado")).getStatusCode().value());
    }

    @Test
    void otraFalla_respondeSinDetalleInterno() {
        var r = handler.handleRuntime(new RuntimeException("detalle interno"));
        assertEquals(500, r.getStatusCode().value());
        assertEquals("Error en el Gateway", r.getBody().get("mensaje"));
        assertEquals("Error interno del servidor", handler.handleGeneric(new Exception("x")).getBody().get("mensaje"));
    }
}
