package Gateway.example.Gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;


@Configuration
public class GatewayRouteConfig {

    @Value("${USERS_SERVICE_URL:http://users-service:8082}")
    private String usersServiceUrl;

    @Value("${ROL_SERVICE_URL:http://rol-service:8081}")
    private String rolServiceUrl;

    @Value("${ESTADO_SERVICE_URL:http://estado-service:8086}")
    private String estadoServiceUrl;

    @Value("${INVENTARIO_SERVICE_URL:http://inventario-service:8083}")
    private String inventarioServiceUrl;

    @Value("${ORDEN_SERVICE_URL:http://orden-service:8084}")
    private String ordenServiceUrl;

    @Value("${PRODUCTO_SERVICE_URL:http://producto-service:8085}")
    private String productoServiceUrl;

    @Value("${CONFIGURACION_SERVICE_URL:http://configuracion-service:8087}")
    private String configuracionServiceUrl;

    // Vacío en el despliegue GCP normal — injectInternalHeaders() no hace nada ahí.
    @Value("${internal.service.key:}")
    private String internalServiceKey;

    // Reenvía a Orden/Producto quién es el usuario ya autenticado por este Gateway.
    private ServerRequest injectInternalHeaders(ServerRequest request) {
        if (internalServiceKey.isBlank()) {
            return request;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ServerRequest.Builder builder = ServerRequest.from(request).header("X-Internal-Key", internalServiceKey);
        if (auth != null && auth.isAuthenticated()) {
            String roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            builder.header("X-User-Id", auth.getName())
                   .header("X-User-Roles", roles);
        }
        return builder.build();
    }

    // ── Users ────────────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> authRoute() {
        return route("auth-route")
                .route(path("/auth/**"), http())
                .before(uri(usersServiceUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> preguntasSeguridadRoute() {
        return route("preguntas-seguridad-route")
                .route(path("/preguntas-seguridad/**"), http())
                .before(uri(usersServiceUrl))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> usersServiceRoute() {
        return route("users-service")
                .route(path("/api/users/**"), http())
                .before(uri(usersServiceUrl))
                .filter(circuitBreaker("usersCircuitBreaker", "/fallback/users"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> direccionesServiceRoute() {
        return route("direcciones-service")
                .route(path("/api/direcciones", "/api/direcciones/**"), http())
                .before(uri(usersServiceUrl))
                .filter(circuitBreaker("usersCircuitBreaker", "/fallback/users"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> regionesServiceRoute() {
        return route("regiones-service")
                .route(path("/api/regiones", "/api/regiones/**"), http())
                .before(uri(usersServiceUrl))
                .filter(circuitBreaker("usersCircuitBreaker", "/fallback/users"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> comunasServiceRoute() {
        return route("comunas-service")
                .route(path("/api/comunas", "/api/comunas/**"), http())
                .before(uri(usersServiceUrl))
                .filter(circuitBreaker("usersCircuitBreaker", "/fallback/users"))
                .build();
    }

    // ── Rol ──────────────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> rolRolesRoute() {
        return route("rol-roles")
                .route(path("/api/roles/**"), http())
                .before(uri(rolServiceUrl))
                .filter(circuitBreaker("rolCircuitBreaker", "/fallback/rol"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rolTiposRoute() {
        return route("rol-tipos")
                .route(path("/api/tipos/**"), http())
                .before(uri(rolServiceUrl))
                .filter(circuitBreaker("rolCircuitBreaker", "/fallback/rol"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rolPrivilegiosRoute() {
        return route("rol-privilegios")
                .route(path("/api/privilegios/**"), http())
                .before(uri(rolServiceUrl))
                .filter(circuitBreaker("rolCircuitBreaker", "/fallback/rol"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rolPermisosRoute() {
        return route("rol-permisos")
                .route(path("/api/permisos/**"), http())
                .before(uri(rolServiceUrl))
                .filter(circuitBreaker("rolCircuitBreaker", "/fallback/rol"))
                .build();
    }

    // ── Estado ───────────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> estadoServiceRoute() {
        return route("estado-service")
                .route(path("/api/estados/**"), http())
                .before(uri(estadoServiceUrl))
                .filter(circuitBreaker("estadoCircuitBreaker", "/fallback/estado"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> tiposEstadoServiceRoute() {
        return route("tipos-estado-service")
                .route(path("/api/tipos-estado/**"), http())
                .before(uri(estadoServiceUrl))
                .filter(circuitBreaker("estadoCircuitBreaker", "/fallback/estado"))
                .build();
    }

    // ── Inventario ───────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> inventarioServiceRoute() {
        return route("inventario-service")
                .route(path("/api/inventario/**"), http())
                .before(uri(inventarioServiceUrl))
                .filter(circuitBreaker("inventarioCircuitBreaker", "/fallback/inventario"))
                .build();
    }

    // ── Orden ────────────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> ordenServiceRoute() {
        return route("orden-service")
                .route(path("/api/ordenes", "/api/ordenes/**"), http())
                .before(this::injectInternalHeaders)
                .before(uri(ordenServiceUrl))
                .filter(circuitBreaker("ordenCircuitBreaker", "/fallback/orden"))
                .build();
    }

    // ── Producto ─────────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> productoServiceRoute() {
        return route("producto-service")
                .route(path("/api/productos/**"), http())
                .before(this::injectInternalHeaders)
                .before(uri(productoServiceUrl))
                .filter(circuitBreaker("productoCircuitBreaker", "/fallback/producto"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> categoriasServiceRoute() {
        return route("categorias-service")
                .route(path("/api/categorias/**"), http())
                .before(this::injectInternalHeaders)
                .before(uri(productoServiceUrl))
                .filter(circuitBreaker("productoCircuitBreaker", "/fallback/producto"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> historialStockServiceRoute() {
        return route("historial-stock-service")
                .route(path("/api/historial-stock", "/api/historial-stock/**"), http())
                .before(uri(productoServiceUrl))
                .filter(circuitBreaker("productoCircuitBreaker", "/fallback/producto"))
                .build();
    }

    // ── Configuracion ────────────────────────────────────────────────────────

    @Bean
    public RouterFunction<ServerResponse> configuracionServiceRoute() {
        return route("configuracion-service")
                .route(path("/api/config/**"), http())
                .before(uri(configuracionServiceUrl))
                .filter(circuitBreaker("configuracionCircuitBreaker", "/fallback/configuracion"))
                .build();
    }
}
