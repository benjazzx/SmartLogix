package User.example.Users.config;

import User.example.Users.security.JwtFilter;
import User.example.Users.security.SmartUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock private JwtFilter jwtFilter;
    @Mock private SmartUserDetailsService userDetailsService;

    @InjectMocks
    private SecurityConfig securityConfig;

    // allowedOrigins se inyecta via @Value en tiempo de ejecucion real; en el test
    // unitario no hay contexto Spring que lo resuelva, asi que queda null salvo que
    // se setee a mano — igual que el mismo bug que ya se arreglo en Gateway.
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:4200");
    }

    @Test
    void passwordEncoder_retornaBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder.matches("test123", encoder.encode("test123")));
    }

    @Test
    void corsConfigurationSource_retornaConfiguracion() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
    }

    @Test
    void authenticationProvider_retornaProvider() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();
        assertNotNull(provider);
    }

    @Test
    void authenticationManager_retornaManager() throws Exception {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        when(config.getAuthenticationManager()).thenReturn(manager);

        AuthenticationManager result = securityConfig.authenticationManager(config);

        assertNotNull(result);
        assertEquals(manager, result);
    }
}
