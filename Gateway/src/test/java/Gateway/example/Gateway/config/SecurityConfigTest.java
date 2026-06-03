package Gateway.example.Gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

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

    @Test
    void filterChain_retornaChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, Answers.RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = securityConfig.filterChain(http);

        assertNotNull(result);
        verify(http).build();
    }

    @Test
    void corsConfigurationSource_permiteLocalhost4200() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertNotNull(source);
        var config = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(config);
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:*"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
    }

    @Test
    void corsConfigurationSource_permiteTodosLosHeaders() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(config);
        assertTrue(config.getAllowedHeaders().contains("*"));
    }

    @Test
    void corsConfigurationSource_permiteMetodosPatch() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(config);
        assertTrue(config.getAllowedMethods().contains("PATCH"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
    }
}
