package Gateway.example.Gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS    = 60_000L;

    // [windowStart, attemptCount]
    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("/auth/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            String ip  = resolveClientIp(request);
            long   now = System.currentTimeMillis();

            attempts.compute(ip, (k, v) -> {
                if (v == null || now - v[0] > WINDOW_MS) return new long[]{now, 1};
                v[1]++;
                return v;
            });

            if (attempts.get(ip)[1] > MAX_ATTEMPTS) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Demasiados intentos. Espera 1 minuto.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
