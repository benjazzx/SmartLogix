package Gateway.example.Gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Autentica las rutas del dominio legado. Acepta dos tipos de token:
 *  1) JWT HMAC propio (login por correo/clave) - comportamiento original.
 *  2) ID token de Cognito (solo si hay un decodificador configurado, perfil aws-ep1):
 *     se valida con el mismo decodificador que usa CognitoSecurityConfig (emisor, audiencia
 *     y firma) y el header Authorization que se reenvia a los servicios se reemplaza por un
 *     JWT legado de corta vida con el rol equivalente, de modo que los servicios de Usuarios,
 *     Inventario, Estados, etc. no necesitan conocer Cognito.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final long LEGACY_TOKEN_TTL_MS = 5 * 60 * 1000L;
    // Orden de prioridad cuando el usuario pertenece a mas de un grupo.
    private static final List<String> GROUP_PRIORITY = List.of("ADMIN", "OPERADOR", "CLIENTE");
    private static final Map<String, String> GROUP_TO_ROL = Map.of(
            "ADMIN", "admin",
            "OPERADOR", "bodeguero",
            "CLIENTE", "cliente"
    );

    private final String jwtSecret;
    private final JwtDecoder cognitoDecoder;

    public JwtAuthFilter(String jwtSecret) {
        this(jwtSecret, null);
    }

    public JwtAuthFilter(String jwtSecret, JwtDecoder cognitoDecoder) {
        this.jwtSecret = jwtSecret;
        this.cognitoDecoder = cognitoDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        HttpServletRequest downstream = request;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                String rolNombre = claims.get("rolNombre", String.class);
                List<GrantedAuthority> authorities = (rolNombre != null && !rolNombre.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + rolNombre))
                    : List.of();
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException legacyError) {
                HttpServletRequest exchanged = exchangeCognitoToken(request, token);
                if (exchanged == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Token inv\u00e1lido o expirado\"}");
                    return;
                }
                downstream = exchanged;
            }
        }
        filterChain.doFilter(downstream, response);
    }

    /** Devuelve la peticion con Authorization reemplazado, o null si no es un ID token Cognito valido. */
    private HttpServletRequest exchangeCognitoToken(HttpServletRequest request, String token) {
        if (cognitoDecoder == null) return null;
        try {
            Jwt jwt = cognitoDecoder.decode(token);
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null) return null;
            String grupo = GROUP_PRIORITY.stream().filter(groups::contains).findFirst().orElse(null);
            if (grupo == null) return null;
            String rol = GROUP_TO_ROL.get(grupo);

            String email = jwt.getClaimAsString("email");
            String subject = (email != null && !email.isBlank()) ? email : jwt.getSubject();
            String legacyToken = Jwts.builder()
                .subject(subject)
                .claim("userId", jwt.getSubject())
                .claim("rolNombre", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + LEGACY_TOKEN_TTL_MS))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

            // Principal = sub de Cognito, igual que en la cadena Cognito (X-User-Id hacia Orden/Producto).
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwt.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            return new AuthorizationOverride(request, "Bearer " + legacyToken);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static class AuthorizationOverride extends HttpServletRequestWrapper {
        private final String value;

        AuthorizationOverride(HttpServletRequest request, String value) {
            super(request);
            this.value = value;
        }

        @Override
        public String getHeader(String name) {
            return "Authorization".equalsIgnoreCase(name) ? value : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return "Authorization".equalsIgnoreCase(name)
                ? Collections.enumeration(List.of(value))
                : super.getHeaders(name);
        }
    }
}