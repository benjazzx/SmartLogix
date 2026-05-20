package Orden.example.Orden.controller;

import Orden.example.Orden.dto.OrdenRequestDto;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
class OrdenControllerTest {

    @InjectMocks
    private OrdenController controller;

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
        sampleResponse.setUserId(userId);
        sampleResponse.setEstadoActual("pendiente");
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
    void createOrden_conTokenValido_retornaOrden() {
        OrdenRequestDto dto = new OrdenRequestDto();
        dto.setDetalles(List.of());
        MockHttpServletRequest request = requestConUserId(userId);

        when(ordenService.createOrden(dto, userId)).thenReturn(sampleResponse);

        ResponseEntity<?> resp = controller.createOrden(dto, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertInstanceOf(OrdenResponseDto.class, resp.getBody());
    }

    @Test
    void createOrden_sinUserId_retorna401() {
        OrdenRequestDto dto = new OrdenRequestDto();
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> resp = controller.createOrden(dto, request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void createOrden_servicioLanzaExcepcion_retorna400ConMensaje() {
        OrdenRequestDto dto = new OrdenRequestDto();
        dto.setDetalles(List.of());
        MockHttpServletRequest request = requestConUserId(userId);

        when(ordenService.createOrden(dto, userId)).thenThrow(new RuntimeException("Stock insuficiente"));

        ResponseEntity<?> resp = controller.createOrden(dto, request);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertInstanceOf(Map.class, resp.getBody());
        assertEquals("Stock insuficiente", ((Map<?, ?>) resp.getBody()).get("error"));
    }

    @Test
    void getMisOrdenes_conTokenValido_retornaLista() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.getMisOrdenes(userId)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<OrdenResponseDto>> resp = controller.getMisOrdenes(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void getMisOrdenes_sinUserId_retorna401() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<List<OrdenResponseDto>> resp = controller.getMisOrdenes(request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void getAll_retornaListaCompleta() {
        when(ordenService.getAll()).thenReturn(List.of(sampleResponse));

        List<OrdenResponseDto> result = controller.getAll();

        assertEquals(1, result.size());
    }

    @Test
    void getById_adminPuedeVerCualquierOrden() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("admin");

        when(ordenService.getById(1L, userId, "admin")).thenReturn(sampleResponse);

        ResponseEntity<OrdenResponseDto> resp = controller.getById(1L, request, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getId());
    }

    @Test
    void getById_ordenNoEncontrada_retorna404() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("cliente");

        when(ordenService.getById(99L, userId, "cliente")).thenThrow(new RuntimeException("No encontrada"));

        ResponseEntity<OrdenResponseDto> resp = controller.getById(99L, request, auth);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getById_accesoDenegado_retorna403() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol("cliente");
        UUID otroUserId = UUID.randomUUID();
        request.setAttribute("userId", otroUserId);

        when(ordenService.getById(2L, otroUserId, "cliente"))
                .thenThrow(new RuntimeException("Acceso denegado: no es tu orden"));

        ResponseEntity<OrdenResponseDto> resp = controller.getById(2L, request, auth);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
