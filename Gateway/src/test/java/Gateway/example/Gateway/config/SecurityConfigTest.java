package Gateway.example.Gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    private static final String SECRET = "SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters";

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "jwtSecret", SECRET);
    }

    private boolean isPublic(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return ReflectionTestUtils.<Boolean>invokeMethod(securityConfig, "isPublic", request);
    }

    @Test
    void rutaAuth_esPublica() {
        assertTrue(isPublic("/auth/login"));
        assertTrue(isPublic("/auth/register"));
    }

    @Test
    void rutaActuator_esPublica() {
        assertTrue(isPublic("/actuator/health"));
        assertTrue(isPublic("/actuator/info"));
    }

    @Test
    void rutaFallback_esPublica() {
        assertTrue(isPublic("/fallback/users"));
        assertTrue(isPublic("/fallback/rol"));
        assertTrue(isPublic("/fallback/orden"));
    }

    @Test
    void rutaErrorExacta_esPublica() {
        assertTrue(isPublic("/error"));
    }

    @Test
    void rutaErrorConSufijo_noEsPublica() {
        assertFalse(isPublic("/error/detalle"));
    }

    @Test
    void rutaApi_noEsPublica() {
        assertFalse(isPublic("/api/usuarios"));
        assertFalse(isPublic("/api/ordenes"));
        assertFalse(isPublic("/api/productos"));
    }

    @Test
    void rutaRaiz_noEsPublica() {
        assertFalse(isPublic("/"));
    }

    @Test
    void rutaArbitraria_noEsPublica() {
        assertFalse(isPublic("/privado/datos"));
    }

    @Test
    void publicFilterChain_retornaChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, Answers.RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = securityConfig.publicFilterChain(http);

        assertNotNull(result);
        verify(http).build();
    }

    @Test
    void protectedFilterChain_retornaChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, Answers.RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = securityConfig.protectedFilterChain(http);

        assertNotNull(result);
        verify(http).build();
    }
}
