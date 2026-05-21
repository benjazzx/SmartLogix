package Orden.example.Orden.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestTemplateConfigTest {

    @Test
    void restTemplate_debeCrearInstancia() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplate rt = config.restTemplate();
        assertNotNull(rt);
    }
}
