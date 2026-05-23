package Orden.example.Orden.config;

import Orden.example.Orden.security.JwtFilter;
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

// Sin UserDetailsService — Orden solo valida tokens JWT emitidos por Users
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROL_CLIENTE       = "cliente";
    private static final String ROL_ADMIN         = "admin";
    private static final String ROL_BODEGUERO     = "bodeguero";
    private static final String ROL_TRANSPORTISTA = "transportista";

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Solo cliente crea ordenes y ve las suyas
                .requestMatchers(HttpMethod.POST, "/api/ordenes").hasRole(ROL_CLIENTE)
                .requestMatchers(HttpMethod.GET,  "/api/ordenes/mis-ordenes").hasRole(ROL_CLIENTE)
                // Admin, bodeguero y transportista ven todas las ordenes y gestionan historial
                .requestMatchers(HttpMethod.GET,  "/api/ordenes").hasAnyRole(ROL_ADMIN, ROL_BODEGUERO, ROL_TRANSPORTISTA)
                // Cliente puede cancelar sus propias órdenes; servicio valida estado y propiedad
                .requestMatchers(HttpMethod.POST, "/api/ordenes/*/historial").hasAnyRole(ROL_ADMIN, ROL_BODEGUERO, ROL_TRANSPORTISTA, ROL_CLIENTE)
                // Ver orden por id e historial: cualquier autenticado (service filtra por propiedad)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
