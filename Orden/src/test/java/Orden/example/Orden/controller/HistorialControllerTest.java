package Orden.example.Orden.controller;

import Orden.example.Orden.dto.HistorialRequestDto;
import Orden.example.Orden.dto.OrdenResponseDto;
import Orden.example.Orden.service.OrdenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class HistorialControllerTest {

    @InjectMocks
    private HistorialController controller;

    @Mock
    private OrdenService ordenService;

    private UUID userId;
    private OrdenResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        sampleResponse = new OrdenResponseDto();
        sampleResponse.setId(1L);
        sampleResponse.setEstadoActual("en_proceso");
        sampleResponse.setDetalles(List.of());
        sampleResponse.setHistorial(List.of());
    }

    private MockHttpServletRequest requestConUserId(UUID uid) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("userId", uid);
        return req;
    }

    private Authentication authConRol(String rol) {
        return new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return rol; }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean b) {}
            @Override public String getName() { return rol; }
        };
    }

    @Test
    void addHistorial_bodegueroAgregaEstado_retornaOk() {
        HistorialRequestDto dto = new HistorialRequestDto();
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("bodeguero");

        when(ordenService.addHistorial(1L, dto, userId, "bodeguero")).thenReturn(sampleResponse);

        ResponseEntity<OrdenResponseDto> resp = controller.addHistorial(1L, dto, request, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("en_proceso", resp.getBody().getEstadoActual());
    }

    @Test
    void addHistorial_ordenNoEncontrada_retorna404() {
        HistorialRequestDto dto = new HistorialRequestDto();
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("admin");

        when(ordenService.addHistorial(99L, dto, userId, "admin"))
                .thenThrow(new RuntimeException("Orden no encontrada"));

        ResponseEntity<OrdenResponseDto> resp = controller.addHistorial(99L, dto, request, auth);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void addHistorial_accesoDenegado_retorna403() {
        HistorialRequestDto dto = new HistorialRequestDto();
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("cliente");

        when(ordenService.addHistorial(1L, dto, userId, "cliente"))
                .thenThrow(new RuntimeException("Acceso denegado: rol insuficiente"));

        ResponseEntity<OrdenResponseDto> resp = controller.addHistorial(1L, dto, request, auth);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void getHistorial_ordenExistente_retornaLista() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("admin");

        OrdenResponseDto.HistorialDto h = new OrdenResponseDto.HistorialDto();
        h.setEstadoNombre("pendiente");
        when(ordenService.getHistorial(1L, userId, "admin")).thenReturn(List.of(h));

        ResponseEntity<List<OrdenResponseDto.HistorialDto>> resp =
                controller.getHistorial(1L, request, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getHistorial_ordenNoEncontrada_retorna404() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("cliente");

        when(ordenService.getHistorial(99L, userId, "cliente"))
                .thenThrow(new RuntimeException("Orden no encontrada"));

        ResponseEntity<List<OrdenResponseDto.HistorialDto>> resp =
                controller.getHistorial(99L, request, auth);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }
}
