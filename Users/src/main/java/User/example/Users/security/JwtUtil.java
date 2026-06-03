package User.example.Users.security;

import User.example.Users.model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long CHALLENGE_EXPIRATION = 5 * 60 * 1000L; // 5 minutos
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_CHALLENGE = "challenge";
    private static final String CLAIM_USER_ID = "userId";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserModel user) {
        return Jwts.builder()
                .subject(user.getCorreo())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim("rolNombre", user.getRolNombre())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }

    /** Token de desafío 2FA — expira en 5 minutos, no sirve como Bearer. */
    public String generateChallengeToken(UserModel user, Long preguntaId) {
        return Jwts.builder()
                .subject(user.getCorreo())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_TYPE, TYPE_CHALLENGE)
                .claim("preguntaId", preguntaId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + CHALLENGE_EXPIRATION))
                .signWith(getSignKey())
                .compact();
    }

    public boolean isChallengeToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TYPE_CHALLENGE.equals(claims.get(CLAIM_TYPE, String.class))
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractPreguntaId(String token) {
        Object raw = extractAllClaims(token).get("preguntaId");
        if (raw instanceof Number n) return n.longValue();
        return null;
    }

    public String extractUserIdClaim(String token) {
        return extractAllClaims(token).get(CLAIM_USER_ID, String.class);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return extractAllClaims(token).get("rolNombre", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
