package Orden.example.Orden.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // El RestTemplate por defecto usa HttpURLConnection, que no soporta PATCH
        // (java.net.ProtocolException: Invalid HTTP method: PATCH). Apache HttpClient5 sí.
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
}
