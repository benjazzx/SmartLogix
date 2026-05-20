package Orden.example.Orden.security;

import io.jsonwebtoken.Claims;
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

class JwtUtilTest {

    private static final String SECRET = "SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String buildToken(String subject, String rolNombre, UUID userId, long expMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(subject)
                .claim("rolNombre", rolNombre)
                .expiration(new Date(System.currentTimeMillis() + expMs));
        if (userId != null) {
            builder.claim("userId", userId.toString());
        }
        return builder.signWith(key).compact();
    }

    @Test
    void extractAllClaims_tokenValido_retornaClaims() {
        String token = buildToken("admin@orden.cl", "admin", UUID.randomUUID(), 3_600_000);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("admin@orden.cl", claims.getSubject());
    }

    @Test
    void extractUsername_retornaCorreo() {
        String token = buildToken("bodeguero@orden.cl", "bodeguero", null, 3_600_000);
        assertEquals("bodeguero@orden.cl", jwtUtil.extractUsername(token));
    }

    @Test
    void extractRol_retornaRolNombre() {
        String token = buildToken("x@orden.cl", "transportista", null, 3_600_000);
        assertEquals("transportista", jwtUtil.extractRol(token));
    }

    @Test
    void extractUserId_conUserId_retornaUUID() {
        UUID userId = UUID.randomUUID();
        String token = buildToken("u@orden.cl", "admin", userId, 3_600_000);
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_sinUserId_retornaNull() {
        String token = buildToken("u@orden.cl", "admin", null, 3_600_000);
        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void isTokenValid_vigente_retornaTrue() {
        assertTrue(jwtUtil.isTokenValid(buildToken("ok@orden.cl", "admin", null, 3_600_000)));
    }

    @Test
    void isTokenValid_expirado_retornaFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject("exp@orden.cl")
                .expiration(new Date(System.currentTimeMillis() - 1000)).signWith(key).compact();
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_malformado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.jwt"));
    }
}
