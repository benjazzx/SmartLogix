package Gateway.example.Gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Segunda validación de JWT (issuer + audience + firma) sobre el ID token de Cognito.
// Solo activo en el perfil aws-ep1; el resto sigue con el JWT propio (SecurityConfig).
@Configuration
@Profile("aws-ep1")
public class CognitoSecurityConfig {

    @Value("${cognito.issuer-uri}")
    private String issuerUri;

    @Value("${cognito.client-id}")
    private String clientId;

    @Bean
    public JwtDecoder cognitoJwtDecoder() {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Collection<String>>(
                "aud", aud -> aud != null && aud.contains(clientId));
        ((org.springframework.security.oauth2.jwt.NimbusJwtDecoder) decoder)
                .setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator));
        return decoder;
    }

    // Grupos de Cognito -> roles internos que ya usan Orden y Producto.
    private static final Map<String, String> ROLE_TRANSLATION = Map.of(
            "ADMIN", "admin",
            "OPERADOR", "bodeguero",
            "CLIENTE", "cliente"
    );

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> cognitoJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null) {
                return defaultConverter.convert(jwt);
            }
            return groups.stream()
                    .map(role -> ROLE_TRANSLATION.getOrDefault(role, role.toLowerCase()))
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain cognitoFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .securityMatcher("/api/ordenes/**", "/api/productos/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                    .decoder(cognitoJwtDecoder())
                    .jwtAuthenticationConverter(cognitoJwtAuthenticationConverter())));
        return http.build();
    }
}
