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

@SuppressWarnings({"java:S100", "java:S1192"})
class OrdenControllerTest {

    private static final String ROL_ADMIN    = "admin";
    private static final String ROL_CLIENTE  = "cliente";
    private static final String MOTIVO_TEST  = "motivo";

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
            @Override public void setAuthenticated(boolean b) { /* not used in tests */ }
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
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol(ROL_ADMIN);
        when(ordenService.getAll(anyString(), any(UUID.class))).thenReturn(List.of(sampleResponse));

        List<OrdenResponseDto> result = controller.getAll(request, auth);

        assertEquals(1, result.size());
    }

    @Test
    void getById_adminPuedeVerCualquierOrden() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol(ROL_ADMIN);

        when(ordenService.getById(1L, userId, ROL_ADMIN)).thenReturn(sampleResponse);

        ResponseEntity<OrdenResponseDto> resp = controller.getById(1L, request, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1L, resp.getBody().getId());
    }

    @Test
    void getById_ordenNoEncontrada_retorna404() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol(ROL_CLIENTE);

        when(ordenService.getById(99L, userId, ROL_CLIENTE)).thenThrow(new RuntimeException("No encontrada"));

        ResponseEntity<OrdenResponseDto> resp = controller.getById(99L, request, auth);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getById_accesoDenegado_retorna403() {
        MockHttpServletRequest request = requestConUserId(userId);
        Authentication auth = authConRol(ROL_CLIENTE);
        UUID otroUserId = UUID.randomUUID();
        request.setAttribute("userId", otroUserId);

        when(ordenService.getById(2L, otroUserId, ROL_CLIENTE))
                .thenThrow(new RuntimeException("Acceso denegado: no es tu orden"));

        ResponseEntity<OrdenResponseDto> resp = controller.getById(2L, request, auth);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ── tomarOrden ────────────────────────────────────────────────────────────

    @Test
    void tomarOrden_conTokenValido_retornaOrden() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.tomarOrden(1L, userId)).thenReturn(sampleResponse);

        ResponseEntity<Object> resp = controller.tomarOrden(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void tomarOrden_sinUserId_retorna401() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Object> resp = controller.tomarOrden(1L, request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void tomarOrden_yaFueTomada_retorna409() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.tomarOrden(1L, userId))
                .thenThrow(new IllegalStateException("La orden ya fue tomada por otro transportista"));

        ResponseEntity<Object> resp = controller.tomarOrden(1L, request);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void tomarOrden_errorGenerico_retorna500() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.tomarOrden(1L, userId))
                .thenThrow(new RuntimeException("Error inesperado"));

        ResponseEntity<Object> resp = controller.tomarOrden(1L, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ── liberarOrden ──────────────────────────────────────────────────────────

    @Test
    void liberarOrden_conTokenValido_retornaOrden() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.liberarOrden(1L, userId)).thenReturn(sampleResponse);

        ResponseEntity<Object> resp = controller.liberarOrden(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void liberarOrden_sinUserId_retorna401() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Object> resp = controller.liberarOrden(1L, request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void liberarOrden_noAutorizado_retorna403() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.liberarOrden(1L, userId))
                .thenThrow(new IllegalStateException("No puedes liberar esta orden"));

        ResponseEntity<Object> resp = controller.liberarOrden(1L, request);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ── solicitarDevolucion ───────────────────────────────────────────────────

    @Test
    void solicitarDevolucion_exitosa_retorna200() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.solicitarDevolucion(1L, userId, "Producto dañado"))
                .thenReturn(sampleResponse);

        ResponseEntity<Object> resp = controller.solicitarDevolucion(
                1L, Map.of(MOTIVO_TEST, "Producto dañado"), request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void solicitarDevolucion_sinUserId_retorna401() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Object> resp = controller.solicitarDevolucion(
                1L, Map.of(MOTIVO_TEST, MOTIVO_TEST), request);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void solicitarDevolucion_estadoInvalido_retorna409() {
        MockHttpServletRequest request = requestConUserId(userId);
        when(ordenService.solicitarDevolucion(anyLong(), any(), anyString()))
                .thenThrow(new IllegalStateException("Solo se puede solicitar devolución de órdenes entregadas"));

        ResponseEntity<Object> resp = controller.solicitarDevolucion(
                1L, Map.of(MOTIVO_TEST, MOTIVO_TEST), request);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // ── resumenEmpleados ──────────────────────────────────────────────────────

    @Test
    void resumenEmpleados_retornaLista() {
        when(ordenService.getResumenEmpleados()).thenReturn(List.of(
                Map.of("empleado", "Juan", "totalAcciones", 5)
        ));

        ResponseEntity<?> resp = controller.resumenEmpleados();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
