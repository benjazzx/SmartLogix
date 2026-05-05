package Estado.example.Estado.Config;

import Estado.example.Estado.Security.JwtFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Lecturas: accesibles dentro de la red Docker (Gateway protege el acceso externo)
                .requestMatchers(HttpMethod.GET, "/api/estados/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tipos-estado/**").permitAll()
                // Escrituras: solo admin con JWT
                .requestMatchers(HttpMethod.POST,   "/api/estados/**").hasRole("admin")
                .requestMatchers(HttpMethod.PUT,    "/api/estados/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/estados/**").hasRole("admin")
                .requestMatchers(HttpMethod.POST,   "/api/tipos-estado/**").hasRole("admin")
                .requestMatchers(HttpMethod.PUT,    "/api/tipos-estado/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/tipos-estado/**").hasRole("admin")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
