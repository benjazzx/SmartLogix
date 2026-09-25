package User.example.Users.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

// Protege la ruta de consulta interna de usuario (llamada por Orden, sin JWT de usuario)
// exigiendo una clave compartida entre servicios en vez de dejarla completamente pública.
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    private static final Pattern INTERNAL_ONLY_URI =
            Pattern.compile("^/api/users/[^/]+/interno$");

    @Value("${internal.service.key}")
    private String expectedKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean isInternalOnly = "GET".equalsIgnoreCase(request.getMethod())
                && INTERNAL_ONLY_URI.matcher(request.getRequestURI()).matches();

        if (isInternalOnly) {
            String key = request.getHeader("X-Internal-Key");
            if (expectedKey == null || !expectedKey.equals(key)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Clave interna inválida\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
