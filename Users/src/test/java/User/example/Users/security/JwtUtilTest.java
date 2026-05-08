package User.example.Users.security;

import User.example.Users.model.UserModel;
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
    private static final long EXPIRATION = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    private UserModel buildUser(String correo, String rolNombre) {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo(correo);
        user.setRolNombre(rolNombre);
        return user;
    }

    private String buildToken(String subject, String rolNombre, long expMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("rolNombre", rolNombre)
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key)
                .compact();
    }

    @Test
    void generateToken_retornaTokenConSubjectCorrecto() {
        UserModel user = buildUser("admin@test.cl", "admin");
        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertEquals("admin@test.cl", jwtUtil.extractUsername(token));
    }

    @Test
    void generateToken_retornaTokenConRolCorrecto() {
        UserModel user = buildUser("bodeguero@test.cl", "bodeguero");
        String token = jwtUtil.generateToken(user);

        assertEquals("bodeguero", jwtUtil.extractRol(token));
    }

    @Test
    void extractAllClaims_tokenValido_retornaClaims() {
        String token = buildToken("usuario@test.cl", "cliente", EXPIRATION);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals("usuario@test.cl", claims.getSubject());
    }

    @Test
    void extractUsername_tokenValido_retornaCorreo() {
        String token = buildToken("emp@empresa.cl", "transportista", EXPIRATION);
        assertEquals("emp@empresa.cl", jwtUtil.extractUsername(token));
    }

    @Test
    void extractRol_tokenConRol_retornaRolNombre() {
        String token = buildToken("x@test.cl", "admin", EXPIRATION);
        assertEquals("admin", jwtUtil.extractRol(token));
    }

    @Test
    void isTokenValid_tokenVigente_retornaTrue() {
        String token = buildToken("valid@test.cl", "admin", EXPIRATION);
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("exp@test.cl")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tokenMalformado_retornaFalse() {
        assertFalse(jwtUtil.isTokenValid("token.invalido.string"));
    }

    @Test
    void isTokenValid_secretoDistinto_retornaFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "OtroSecretoCompletamenteDiferenteParaTestear1234".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("hack@test.cl")
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(otherKey)
                .compact();
        assertFalse(jwtUtil.isTokenValid(token));
    }
}
