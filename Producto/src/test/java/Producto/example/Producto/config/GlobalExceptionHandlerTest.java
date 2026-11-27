package Producto.example.Producto.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** D-02: las entradas inválidas responden 4xx y nunca revelan detalles internos. */
class GlobalExceptionHandlerTest {

    @RestController
    static class ControladorDePrueba {
        @PostMapping("/eco")
        public Object eco(@RequestBody Map<String, Object> cuerpo) {
            return cuerpo;
        }

        @GetMapping("/item/{id}")
        public Object item(@PathVariable("id") UUID id) {
            return id;
        }

        @GetMapping("/buscar")
        public Object buscar(@RequestParam("texto") String texto) {
            return texto;
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(handler).build();

    @Test
    void jsonMalFormado_responde400SinDetalle() throws Exception {
        mockMvc.perform(post("/eco").contentType(MediaType.APPLICATION_JSON).content("{mal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud tiene datos inválidos o mal formados"));
    }

    @Test
    void identificadorInvalido_responde400() throws Exception {
        mockMvc.perform(get("/item/no-es-un-id")).andExpect(status().isBadRequest());
    }

    @Test
    void parametroAusente_responde400() throws Exception {
        mockMvc.perform(get("/buscar")).andExpect(status().isBadRequest());
    }

    @Test
    void metodoNoPermitido_responde405() throws Exception {
        mockMvc.perform(post("/buscar")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void errorDeProgramacion_noRevelaElMensaje() {
        var r = handler.handleRuntime(new NullPointerException("Cannot invoke Categoria.getId()"));
        assertEquals(400, r.getStatusCode().value());
        assertFalse(String.valueOf(r.getBody().get("mensaje")).contains("Categoria"));
    }

    @Test
    void reglasDeNegocio_conservanElMensaje() {
        assertEstado(404, handler.handleRuntime(new RuntimeException("Producto no encontrado")));
        assertEstado(409, handler.handleRuntime(new RuntimeException("stock insuficiente")));
        assertEstado(400, handler.handleRuntime(new RuntimeException("Precio inválido")));
        assertEstado(410, handler.handleRuntime(new ResponseStatusException(HttpStatus.GONE)));
    }

    @Test
    void demasExcepciones_tienenSuCodigo() throws Exception {
        assertEstado(404, handler.handleNotFound(new EntityNotFoundException("x")));
        assertEstado(409, handler.handleConflict(new IllegalArgumentException("x")));
        assertEstado(403, handler.handleAccesoDenegado(new AccessDeniedException("x")));
        assertEstado(409, handler.handleIntegridad(new DataIntegrityViolationException("x")));
        assertEstado(500, handler.handleGeneric(new Exception("detalle interno")));
        var resultado = new BeanPropertyBindingResult(new Object(), "dto");
        resultado.addError(new FieldError("dto", "nombre", "obligatorio"));
        var parametro = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
        assertEstado(400, handler.handleValidation(new MethodArgumentNotValidException(parametro, resultado)));
    }

    private void assertEstado(int esperado, ResponseEntity<?> respuesta) {
        assertEquals(esperado, respuesta.getStatusCode().value());
    }
}
