package Producto.example.Producto.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Confía en la identidad que ya resolvió el BFF (Gateway), validada solo si viene
// con la clave interna correcta (misma clave que InternalKeyFilter).
@Component
public class BffTrustFilter extends OncePerRequestFilter {

    @Value("${internal.service.key:}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader("X-Internal-Key");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (!expectedKey.isBlank() && expectedKey.equals(key) && rolesHeader != null && !rolesHeader.isBlank()) {
            List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            String userId = request.getHeader("X-User-Id");
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // El controller lee el UUID del usuario desde este atributo, igual que JwtFilter.
            if (userId != null) {
                try {
                    request.setAttribute("userId", UUID.fromString(userId));
                } catch (IllegalArgumentException ignored) {
                    // no era un UUID válido, se deja sin setear
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
