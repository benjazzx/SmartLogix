package Estado.example.Estado.security;

import Estado.example.Estado.Security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "SmartLogixSuperSecretKeyForJWT2024MustBeAtLeast32Characters";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String buildToken(String subject, String rolNombre, long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("rolNombre", rolNombre)
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    private String buildExpiredToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();
    }

    @Test
    void extractAllClaims_tokenValido_retornaClaims() {
        String token = buildToken("usuario@estado.cl", "admin", 3_600_000);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("usuario@estado.cl", claims.getSubject());
    }

    @Test
    void extractRol_tokenConRol_retornaRol() {
        String token = buildToken("emp@estado.cl", "transportista", 3_600_000);
        assertEquals("transportista", jwtUtil.extractRol(token));
    }

    @Test
    void extractRol_tokenSinClaim_retornaNull() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("sin-rol@test.cl")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
        assertNull(jwtUtil.extractRol(token));
    }

    @Test
    void isTokenValid_tokenVigente_retornaTrue() {
        assertTrue(jwtUtil.isTokenValid(buildToken("ok@test.cl", "admin", 3_600_000)));
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid(buildExpiredToken("exp@test.cl")));
    }

    @Test
    void isTokenValid_tokenMalformado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid("no.es.jwt"));
    }

    @Test
    void isTokenValid_secretoDistinto_retornaFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "OtroSecretoCompletamenteDiferenteParaTestear1234".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("hack@test.cl")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(otherKey)
                .compact();
        assertFalse(jwtUtil.isTokenValid(token));
    }
}
