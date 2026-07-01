package Gateway.example.Gateway.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

class FallbackControllerTest {

    private FallbackController fallbackController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
        mockMvc = MockMvcBuilders.standaloneSetup(fallbackController).build();
    }

    @Test
    void usersFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/users"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("users-service"));
    }

    @Test
    void rolFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/rol"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("rol-service"));
    }

    @Test
    void estadoFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/estado"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("estado-service"));
    }

    @Test
    void inventarioFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/inventario"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("inventario-service"));
    }

    @Test
    void ordenFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/orden"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("orden-service"));
    }

    @Test
    void productoFallback_retorna503ConMensaje() throws Exception {
        mockMvc.perform(get("/fallback/producto"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.service").value("producto-service"));
    }

    @Test
    void usersFallback_cuerpoContieneError() {
        ResponseEntity<Map<String, String>> response = fallbackController.usersFallback();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().containsKey("service"));
    }

    @Test
    void todosLosFallbacks_retornanStatusCorrecto() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.usersFallback().getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.rolFallback().getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.estadoFallback().getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.inventarioFallback().getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.ordenFallback().getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackController.productoFallback().getStatusCode());
    }
}
