package User.example.Users.config;

import User.example.Users.security.InternalKeyFilter;
import User.example.Users.security.JwtFilter;
import User.example.Users.security.SmartUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired private JwtFilter jwtFilter;
    @Autowired private InternalKeyFilter internalKeyFilter;
    @Autowired private SmartUserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
            // Sin .cors() a propósito: este servicio nunca lo llama un navegador
            // directamente, solo el BFF (proxy interno) — es el BFF el que ya agrega
            // el header Access-Control-Allow-Origin para el cliente. Si este servicio
            // TAMBIÉN lo agregara, el header quedaría duplicado en la respuesta que el
            // BFF reenvía, y el navegador la rechaza por spec.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Geo: regiones y comunas accesibles a cualquier autenticado
                .requestMatchers(HttpMethod.GET,    "/api/regiones/**").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/comunas/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/direcciones").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/direcciones/**").authenticated()
                // Cualquier usuario autenticado puede ver/editar su propio perfil
                .requestMatchers(HttpMethod.GET,    "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT,    "/api/users/me").authenticated()
                // Consulta interna servicio-a-servicio (Orden), protegida por InternalKeyFilter, no por JWT
                .requestMatchers(HttpMethod.GET,    "/api/users/*/interno").permitAll()
                // Solo ADMIN puede hacer CRUD de usuarios
                .requestMatchers(HttpMethod.POST,   "/api/users/**").hasRole("admin")
                .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("admin")
                .requestMatchers(HttpMethod.GET,    "/api/users/**").hasRole("admin")
                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
