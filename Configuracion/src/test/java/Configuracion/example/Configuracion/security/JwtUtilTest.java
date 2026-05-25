package Configuracion.example.Configuracion.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S100")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters";
    private static final UUID   USER_ID = UUID.randomUUID();
    private static final String CORREO  = "test@smartlogix.cl";
    private static final String ROL     = "admin";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String buildToken(long expireMillis) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(CORREO)
                .claim("rolNombre", ROL)
                .claim("userId", USER_ID.toString())
                .expiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(key)
                .compact();
    }

    @Test
    void extractUsername_retornaCorreo() {
        assertEquals(CORREO, jwtUtil.extractUsername(buildToken(60_000)));
    }

    @Test
    void extractRol_retornaRol() {
        assertEquals(ROL, jwtUtil.extractRol(buildToken(60_000)));
    }

    @Test
    void extractUserId_retornaUUID() {
        assertEquals(USER_ID, jwtUtil.extractUserId(buildToken(60_000)));
    }

    @Test
    void isTokenValid_tokenValido_retornaTrue() {
        assertTrue(jwtUtil.isTokenValid(buildToken(60_000)));
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid(buildToken(-1_000)));
    }

    @Test
    void isTokenValid_tokenInvalido_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid("token.completamente.invalido"));
    }
}
