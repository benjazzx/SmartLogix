package Producto.example.Producto.config;

import Producto.example.Producto.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentación pública
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Lectura: todos los roles autenticados (incluye Inventario vía ProductoClient)
                .requestMatchers(HttpMethod.GET, "/api/productos/**").hasAnyRole("admin", "bodeguero", "transportista", "cliente")
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").hasAnyRole("admin", "bodeguero", "transportista", "cliente")
                // Escritura: solo admin y bodeguero
                .requestMatchers(HttpMethod.POST,   "/api/productos/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.PUT,    "/api/productos/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.PATCH,  "/api/productos/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.POST,   "/api/categorias/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.PUT,    "/api/categorias/**").hasAnyRole("admin", "bodeguero")
                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**").hasAnyRole("admin")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
