package Gateway.example.Gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GatewayRouteConfigTest {

    private GatewayRouteConfig config;

    @BeforeEach
    void setUp() {
        config = new GatewayRouteConfig();
        ReflectionTestUtils.setField(config, "usersServiceUrl", "http://users-service:8082");
        ReflectionTestUtils.setField(config, "internalServiceKey", "");
    }

    @Test
    void rabbitAdminRoute_seConstruye() {
        RouterFunction<ServerResponse> route = config.rabbitAdminRoute();

        assertNotNull(route);
    }

    @Test
    void rabbitAdminRoute_noEsRutaPublica() {
        SecurityConfig security = new SecurityConfig();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rabbitmq/queues");
        request.setRequestURI("/api/rabbitmq/queues");

        Boolean publica = ReflectionTestUtils.invokeMethod(security, "isPublic", request);

        assertFalse(Boolean.TRUE.equals(publica));
    }
}
