package Rol.example.Rol.config;

import org.springframework.web.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        log.warn("[Rol] Recurso no encontrado: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalArgumentException ex) {
        log.warn("[Rol] Argumento inválido: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.info("[Rol] Validación fallida para campos: {}", errores.keySet());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validación fallida");
        body.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        // Errores estándar de Spring MVC (método no permitido, parámetro ausente, ruta inexistente...)
        if (ex instanceof ErrorResponse respuesta) {
            return buildError(HttpStatus.valueOf(respuesta.getStatusCode().value()),
                    "La solicitud no es válida para este recurso");
        }
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("no encontrado") || msg.contains("not found")) {
            log.warn("[Rol] No encontrado: {}", msg);
            return buildError(HttpStatus.NOT_FOUND, msg);
        }
        if (msg.contains("Ya existe") || msg.contains("duplicado")) {
            log.warn("[Rol] Conflicto: {}", msg);
            return buildError(HttpStatus.CONFLICT, msg);
        }
        // Fallas no previstas (errores de programación o de base de datos): sin detalle interno.
        if (ex.getClass() != RuntimeException.class && !(ex instanceof IllegalStateException)) {
            log.error("[Rol] Falla no prevista", ex);
            return buildError(HttpStatus.BAD_REQUEST, "La solicitud no se pudo procesar");
        }
        log.error("[Rol] RuntimeException inesperada: {}", msg);
        return buildError(HttpStatus.BAD_REQUEST, msg);
    }

    // D-02: una entrada inválida del cliente nunca termina en 500 ni revela detalles internos.
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<Map<String, Object>> handleEntradaInvalida(Exception ex) {
        log.warn("[Rol] Entrada inválida: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "La solicitud tiene datos inválidos o mal formados");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridad(DataIntegrityViolationException ex) {
        log.warn("[Rol] Restricción de datos: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "No se pudo guardar: el dato ya existe o falta un dato obligatorio");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Errores estándar de Spring MVC (método no permitido, parámetro ausente, ruta inexistente...)
        if (ex instanceof ErrorResponse respuesta) {
            return buildError(HttpStatus.valueOf(respuesta.getStatusCode().value()),
                    "La solicitud no es válida para este recurso");
        }
        log.error("[Rol] Error interno no controlado", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
