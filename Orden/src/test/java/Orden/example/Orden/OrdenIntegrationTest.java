package Orden.example.Orden;

import Orden.example.Orden.client.ProductoClient;
import Orden.example.Orden.client.UsersClient;
import Orden.example.Orden.exception.ServicioOcupadoException;
import Orden.example.Orden.messaging.LogisticaRabbitProducer;
import Orden.example.Orden.messaging.OrdenEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Prueba de integracion real: levanta el contexto completo de Spring (controller,
// security filter chain, service, repository JPA) contra una base H2 en memoria.
// Solo se mockean los colaboradores externos (Users y Producto, otros microservicios)
// para no depender de que esten corriendo; todo lo demas es codigo real de Orden.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenIntegrationTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UsersClient usersClient;

    @MockitoBean
    private ProductoClient productoClient;

    @MockitoBean
    private OrdenEventProducer eventProducer;

    @MockitoBean
    private LogisticaRabbitProducer logisticaRabbitProducer;

    private UUID userId;
    private UUID productoId;
    private UUID direccionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productoId = UUID.randomUUID();
        direccionId = UUID.randomUUID();

        org.mockito.Mockito.when(usersClient.getNombreUsuario(userId)).thenReturn("Cliente de Prueba");
        org.mockito.Mockito.when(usersClient.getDireccionTexto(userId)).thenReturn("Calle Falsa 123, Santiago");
        org.mockito.Mockito.when(productoClient.getProducto(productoId)).thenReturn(Map.of(
                "nombre", "Teclado Mecanico USB",
                "precio", 29990
        ));
        org.mockito.Mockito.when(productoClient.reservarStock(org.mockito.Mockito.eq(productoId),
                org.mockito.Mockito.anyInt(), org.mockito.Mockito.any())).thenReturn(true);
    }

    @Test
    void crearOrden_sinAutenticacion_debeRechazarConForbidden() throws Exception {
        String body = bodyConUnDetalle(1);

        mockMvc.perform(post("/api/ordenes")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearOrden_comoCliente_debePersistirYQuedarDisponibleEnMisOrdenes() throws Exception {
        String body = bodyConUnDetalle(2);

        mockMvc.perform(post("/api/ordenes")
                        .contentType("application/json")
                        .content(body)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "ROLE_cliente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.estadoActual").value("pendiente"))
                .andExpect(jsonPath("$.detalles[0].productoNombre").value("Teclado Mecanico USB"));

        // La orden recien creada debe aparecer de verdad en el listado del cliente —
        // confirma que el POST escribio en la base H2 real, no solo que respondio 200.
        mockMvc.perform(get("/api/ordenes/mis-ordenes")
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "ROLE_cliente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));

        org.mockito.Mockito.verify(eventProducer).publishOrdenCreada(org.mockito.Mockito.any());
    }

    @Test
    void crearOrden_rolSinPermiso_debeRechazarConForbidden() throws Exception {
        String body = bodyConUnDetalle(1);

        mockMvc.perform(post("/api/ordenes")
                        .contentType("application/json")
                        .content(body)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "ROLE_transportista"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearOrden_sinStockDisponible_debeRetornarBadRequestConMensajeClaro() throws Exception {
        org.mockito.Mockito.when(productoClient.reservarStock(org.mockito.Mockito.eq(productoId),
                org.mockito.Mockito.anyInt(), org.mockito.Mockito.any())).thenReturn(false);

        String body = bodyConUnDetalle(100);

        mockMvc.perform(post("/api/ordenes")
                        .contentType("application/json")
                        .content(body)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "ROLE_cliente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
    }

    @Test
    void crearOrden_productoClientFallaTransitoriamente_debeRetornar503NoBadRequest() throws Exception {
        // Simula lo que de verdad paso en la prueba de pico (1000 solicitudes): el circuit
        // breaker hacia Producto se abre y ProductoClient lanza ServicioOcupadoException en
        // vez de devolver null -- antes esto se reportaba igual que "producto no existe" (400).
        org.mockito.Mockito.when(productoClient.getProducto(productoId))
                .thenThrow(new ServicioOcupadoException(
                        "El servicio de productos está temporalmente saturado. Intenta de nuevo en unos segundos."));

        String body = bodyConUnDetalle(1);

        mockMvc.perform(post("/api/ordenes")
                        .contentType("application/json")
                        .content(body)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "ROLE_cliente"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("saturado")));
    }

    private String bodyConUnDetalle(int cantidad) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "direccionId", direccionId.toString(),
                "userNombre", "Cliente de Prueba",
                "detalles", new Object[] {
                        Map.of("productoId", productoId.toString(), "cantidad", cantidad)
                }
        ));
    }
}
