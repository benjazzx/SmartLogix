package Inventario.example.Inventario.security;

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
        String token = buildToken("user@inv.cl", "bodeguero", UUID.randomUUID(), 3_600_000);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("user@inv.cl", claims.getSubject());
    }

    @Test
    void extractUsername_retornaCorreo() {
        String token = buildToken("bodeguero@test.cl", "bodeguero", UUID.randomUUID(), 3_600_000);
        assertEquals("bodeguero@test.cl", jwtUtil.extractUsername(token));
    }

    @Test
    void extractRol_retornaRolNombre() {
        String token = buildToken("x@test.cl", "admin", UUID.randomUUID(), 3_600_000);
        assertEquals("admin", jwtUtil.extractRol(token));
    }

    @Test
    void extractUserId_tokenConUserId_retornaUUID() {
        UUID userId = UUID.randomUUID();
        String token = buildToken("u@test.cl", "admin", userId, 3_600_000);
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_tokenSinUserId_retornaNull() {
        String token = buildToken("u@test.cl", "admin", null, 3_600_000);
        assertNull(jwtUtil.extractUserId(token));
    }

    @Test
    void isTokenValid_tokenVigente_retornaTrue() {
        assertTrue(jwtUtil.isTokenValid(buildToken("ok@test.cl", "admin", null, 3_600_000)));
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject("exp@test.cl")
                .expiration(new Date(System.currentTimeMillis() - 1000)).signWith(key).compact();
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tokenMalformado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid("no.es.un.jwt.valido"));
    }
}
