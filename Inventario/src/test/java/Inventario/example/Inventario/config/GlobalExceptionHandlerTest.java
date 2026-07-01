package Inventario.example.Inventario.config;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_retorna404() {
        EntityNotFoundException ex = new EntityNotFoundException("Bodega no encontrada");

        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Bodega no encontrada", resp.getBody().get("mensaje"));
        assertEquals(404, resp.getBody().get("status"));
    }

    @Test
    void handleBadRequest_retorna409() {
        IllegalArgumentException ex = new IllegalArgumentException("Nombre duplicado");

        ResponseEntity<Map<String, Object>> resp = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("Nombre duplicado", resp.getBody().get("mensaje"));
    }

    @Test
    void handleGeneric_retorna500() {
        RuntimeException ex = new RuntimeException("Error inesperado");

        ResponseEntity<Map<String, Object>> resp = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertTrue(resp.getBody().get("mensaje").toString().contains("Error interno"));
    }

    @Test
    void handleValidation_retorna400ConCampos() {
        FieldError fieldError = new FieldError("bodegaRequest", "nombre", "El nombre es obligatorio");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(400, resp.getBody().get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) resp.getBody().get("errores");
        assertNotNull(errores);
        assertTrue(errores.containsKey("nombre"));
        assertNotNull(resp.getBody().get("timestamp"));
    }
}
