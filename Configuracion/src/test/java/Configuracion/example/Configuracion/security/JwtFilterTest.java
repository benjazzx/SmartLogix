package Configuracion.example.Configuracion.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtUtil    jwtUtil;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtFilter jwtFilter;

    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeader_continua_sinAutenticacion() throws Exception {
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerSinBearer_continua_sinAutenticacion() throws Exception {
        request.addHeader("Authorization", "Basic abc123");
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenInvalido_continua_sinAutenticacion() throws Exception {
        request.addHeader("Authorization", "Bearer token.invalido");
        when(jwtUtil.isTokenValid("token.invalido")).thenReturn(false);
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValido_conUserId_setAtributoYAutenticacion() throws Exception {
        UUID   userId = UUID.randomUUID();
        String token  = "token.valido";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("user@smartlogix.cl");
        when(jwtUtil.extractRol(token)).thenReturn("admin");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(userId, request.getAttribute("userId"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenValido_sinUserId_noSetAtributo() throws Exception {
        String token = "token.sin.userid";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("user@smartlogix.cl");
        when(jwtUtil.extractRol(token)).thenReturn("cliente");
        when(jwtUtil.extractUserId(token)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(request.getAttribute("userId"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
