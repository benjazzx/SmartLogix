package Configuracion.example.Configuracion.controller;

import Configuracion.example.Configuracion.service.ConfiguracionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
@ExtendWith(MockitoExtension.class)
class ConfiguracionControllerTest {

    @Mock private ConfiguracionService service;
    @Mock private HttpServletRequest request;
    @InjectMocks private ConfiguracionController controller;

    private UUID userId;

    @BeforeEach
    void setUp() { userId = UUID.randomUUID(); }

    @Test
    void getPreferencias_sinUserId_retorna401() {
        when(request.getAttribute("userId")).thenReturn(null);
        ResponseEntity<Map<String, String>> res = controller.getPreferencias(request);
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void getPreferencias_conUserId_retornaPreferencias() {
        when(request.getAttribute("userId")).thenReturn(userId);
        when(service.getPreferencias(userId)).thenReturn(Map.of("tema", "esmeralda"));
        ResponseEntity<Map<String, String>> res = controller.getPreferencias(request);
        assertEquals(200, res.getStatusCode().value());
        assertEquals("esmeralda", res.getBody().get("tema"));
    }

    @Test
    void setPreferencia_sinUserId_retorna401() {
        when(request.getAttribute("userId")).thenReturn(null);
        ResponseEntity<Void> res = controller.setPreferencia(request, "tema", Map.of("valor", "azul"));
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void setPreferencia_sinValor_retorna400() {
        when(request.getAttribute("userId")).thenReturn(userId);
        ResponseEntity<Void> res = controller.setPreferencia(request, "tema", Map.of());
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void setPreferencia_valorBlanco_retorna400() {
        when(request.getAttribute("userId")).thenReturn(userId);
        ResponseEntity<Void> res = controller.setPreferencia(request, "tema", Map.of("valor", "  "));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void setPreferencia_valido_retorna200() {
        when(request.getAttribute("userId")).thenReturn(userId);
        ResponseEntity<Void> res = controller.setPreferencia(request, "tema", Map.of("valor", "oceano"));
        assertEquals(200, res.getStatusCode().value());
        verify(service).setPreferencia(userId, "tema", "oceano");
    }
}
