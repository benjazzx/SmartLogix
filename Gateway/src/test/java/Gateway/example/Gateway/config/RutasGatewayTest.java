package Gateway.example.Gateway.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ServletRequestPathUtils;

/** Cada ruta del BFF atiende su prefijo y ningún otro. */
class RutasGatewayTest {

    private final GatewayRouteConfig config = new GatewayRouteConfig();

    @BeforeEach
    void setUp() {
        for (String campo : List.of("usersServiceUrl", "rolServiceUrl", "estadoServiceUrl", "inventarioServiceUrl",
                "ordenServiceUrl", "productoServiceUrl", "configuracionServiceUrl")) {
            ReflectionTestUtils.setField(config, campo, "http://servicio:8080");
        }
        ReflectionTestUtils.setField(config, "internalServiceKey", "");
    }

    @ParameterizedTest
    @CsvSource({
            "authRoute, /auth/login",
            "preguntasSeguridadRoute, /preguntas-seguridad/1",
            "usersServiceRoute, /api/users/me",
            "direccionesServiceRoute, /api/direcciones",
            "regionesServiceRoute, /api/regiones/1",
            "comunasServiceRoute, /api/comunas",
            "rabbitAdminRoute, /api/rabbitmq/queues",
            "rolRolesRoute, /api/roles/assign",
            "rolTiposRoute, /api/tipos/1",
            "rolPrivilegiosRoute, /api/privilegios/1",
            "rolPermisosRoute, /api/permisos/1",
            "estadoServiceRoute, /api/estados/1",
            "tiposEstadoServiceRoute, /api/tipos-estado/1",
            "inventarioServiceRoute, /api/inventario/bodegas",
            "ordenServiceRoute, /api/ordenes/1",
            "productoServiceRoute, /api/productos/1",
            "categoriasServiceRoute, /api/categorias/1",
            "historialStockServiceRoute, /api/historial-stock",
            "configuracionServiceRoute, /api/config/tema"
    })
    void cadaRutaAtiendeSoloSuPrefijo(String bean, String ruta) {
        RouterFunction<ServerResponse> router = ReflectionTestUtils.invokeMethod(config, bean);

        assertTrue(router.route(peticion(ruta)).isPresent());
        assertFalse(router.route(peticion("/ruta-que-no-existe")).isPresent());
    }

    private ServerRequest peticion(String ruta) {
        MockHttpServletRequest servlet = new MockHttpServletRequest("GET", ruta);
        servlet.setRequestURI(ruta);
        ServletRequestPathUtils.parseAndCache(servlet);
        return ServerRequest.create(servlet, List.of());
    }
}
