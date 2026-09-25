package Producto.example.Producto.config;

import Producto.example.Producto.security.BffTrustFilter;
import Producto.example.Producto.security.InternalKeyFilter;
import Producto.example.Producto.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Producto solo valida tokens JWT emitidos por Users
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_ADMIN     = "admin";
    private static final String ROLE_BODEGUERO = "bodeguero";

    private final JwtFilter jwtFilter;
    private final InternalKeyFilter internalKeyFilter;
    private final BffTrustFilter bffTrustFilter;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Solo el BFF llama a este servicio, no el navegador — el CORS lo pone el BFF.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentación pública
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // /error permite que Spring reenvíe excepciones correctamente (sin esto devuelve 403 sobre el error real)
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Historial de stock: información sensible de auditoría — solo admin/bodeguero
                .requestMatchers(HttpMethod.GET, "/api/productos/*/historial-stock").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.GET, "/api/historial-stock/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                // GET del catálogo: público dentro de la red Docker (el Gateway protege el acceso externo)
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                // Descuento de stock: llamado internamente por Inventario (sin JWT de usuario).
                // La autorización real la hace InternalKeyFilter (clave compartida X-Internal-Key).
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/decrementar-stock").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/registrar-devolucion").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/reservar-stock").permitAll()
                // Escritura: solo admin y bodeguero (con JWT)
                .requestMatchers(HttpMethod.POST,   "/api/productos/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.PUT,    "/api/productos/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.PATCH,  "/api/productos/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.POST,   "/api/categorias/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.PUT,    "/api/categorias/**").hasAnyRole(ROLE_ADMIN, ROLE_BODEGUERO)
                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**").hasAnyRole(ROLE_ADMIN)
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(bffTrustFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
