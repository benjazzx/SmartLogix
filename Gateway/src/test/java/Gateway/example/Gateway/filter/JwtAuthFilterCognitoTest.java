package Gateway.example.Gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterCognitoTest {

    private static final String SECRET = "clave-de-prueba-para-jwt-legado-de-al-menos-32-bytes";

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Jwt idTokenCognito(List<String> grupos) {
        Jwt.Builder b = Jwt.withTokenValue("token-cognito")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("email", "admin@smartlogix.cl")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600));
        if (grupos != null) b.claim("cognito:groups", grupos);
        return b.build();
    }

    private MockHttpServletRequest conBearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    void tokenCognitoValido_reemplazaAuthorizationPorJwtLegadoConRolDeMayorPrivilegio() throws Exception {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("token-cognito")).thenReturn(idTokenCognito(List.of("CLIENTE", "ADMIN")));
        JwtAuthFilter filter = new JwtAuthFilter(SECRET, decoder);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(conBearer("token-cognito"), res, chain);

        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
        verify(chain).doFilter(captor.capture(), any());
        String header = ((HttpServletRequest) captor.getValue()).getHeader("Authorization");
        assertTrue(header.startsWith("Bearer "));
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(header.substring(7)).getPayload();
        assertEquals("admin", claims.get("rolNombre", String.class));
        assertEquals("admin@smartlogix.cl", claims.getSubject());
        assertEquals("sub-123", claims.get("userId", String.class));
        assertEquals("sub-123", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void tokenCognitoInvalido_responde401YNoContinuaLaCadena() throws Exception {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("token-malo")).thenThrow(new BadJwtException("firma invalida"));
        JwtAuthFilter filter = new JwtAuthFilter(SECRET, decoder);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(conBearer("token-malo"), res, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void tokenCognitoSinGrupos_responde401() throws Exception {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode("token-cognito")).thenReturn(idTokenCognito(null));
        JwtAuthFilter filter = new JwtAuthFilter(SECRET, decoder);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(conBearer("token-cognito"), res, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sinDecodificadorCognito_untokenAjeno_responde401() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(SECRET);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(conBearer("token-cognito"), res, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}