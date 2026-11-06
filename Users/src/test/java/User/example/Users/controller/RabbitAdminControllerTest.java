package User.example.Users.controller;

import User.example.Users.config.GlobalExceptionHandler;
import User.example.Users.dto.BindingRequestDto;
import User.example.Users.dto.QueueRequestDto;
import User.example.Users.service.RabbitAdminService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RabbitAdminControllerTest {

    @Mock private RabbitAdminService service;
    @InjectMocks private RabbitAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Colas ────────────────────────────────────────────────────────────────

    @Test
    void crearQueue_valida_retorna201() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola.prueba-1\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<QueueRequestDto> captor = ArgumentCaptor.forClass(QueueRequestDto.class);
        verify(service).crearQueue(captor.capture());
        assertEquals("cola.prueba-1", captor.getValue().getNombre());
        assertEquals(true, captor.getValue().isDurable());
    }

    @Test
    void crearQueue_conDeadLetter_pasaLosCampos() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola\",\"deadLetterExchange\":\"mi-dlx\","
                                + "\"deadLetterRoutingKey\":\"cola.dlq\",\"ttlMs\":5000}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<QueueRequestDto> captor = ArgumentCaptor.forClass(QueueRequestDto.class);
        verify(service).crearQueue(captor.capture());
        assertEquals("mi-dlx", captor.getValue().getDeadLetterExchange());
        assertEquals(5000, captor.getValue().getTtlMs());
    }

    @Test
    void crearQueue_nombreVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).crearQueue(any());
    }

    @Test
    void crearQueue_nombreConCaracteresInvalidos_retorna400() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola con espacios!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearQueue_routingKeyDeadLetterSinExchange_retorna400() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola\",\"deadLetterRoutingKey\":\"cola.dlq\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).crearQueue(any());
    }

    @Test
    void crearQueue_ttlCeroONegativo_retorna400() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cola\",\"ttlMs\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarQueue_existente_retorna204() throws Exception {
        mockMvc.perform(delete("/api/rabbitmq/queues/cola-vieja"))
                .andExpect(status().isNoContent());

        verify(service).eliminarQueue("cola-vieja");
    }

    @Test
    void eliminarQueue_noExiste_retorna404() throws Exception {
        doThrow(new EntityNotFoundException("Cola no encontrada: x")).when(service).eliminarQueue("x");

        mockMvc.perform(delete("/api/rabbitmq/queues/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarQueue_protegida_retorna400() throws Exception {
        doThrow(new IllegalStateException("'orden-bodega-queue' es un recurso que usa la propia aplicación"))
                .when(service).eliminarQueue("orden-bodega-queue");

        mockMvc.perform(delete("/api/rabbitmq/queues/orden-bodega-queue"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarQueues_retornaLaLista() throws Exception {
        when(service.listarQueues()).thenReturn(List.of(Map.of("nombre", "cola-a", "protegida", false)));

        mockMvc.perform(get("/api/rabbitmq/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("cola-a"));
    }

    // ── Exchanges ────────────────────────────────────────────────────────────

    @Test
    void crearExchange_tipoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ex\",\"tipo\":\"headers\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearExchange_valido_retorna201() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ex\",\"tipo\":\"direct\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarExchange_retorna204() throws Exception {
        mockMvc.perform(delete("/api/rabbitmq/exchanges/ex"))
                .andExpect(status().isNoContent());

        verify(service).eliminarExchange("ex");
    }

    @Test
    void listarExchanges_retornaLaLista() throws Exception {
        when(service.listarExchanges()).thenReturn(List.of(Map.of("nombre", "ex", "tipo", "topic")));

        mockMvc.perform(get("/api/rabbitmq/exchanges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("topic"));
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Test
    void crearBinding_valido_retorna201() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cola\":\"cola-a\",\"exchange\":\"ex-a\",\"routingKey\":\"a.*\"}"))
                .andExpect(status().isCreated());

        verify(service).crearBinding(any(BindingRequestDto.class));
    }

    @Test
    void eliminarBinding_porQueryParams_retorna204() throws Exception {
        mockMvc.perform(delete("/api/rabbitmq/bindings")
                        .param("cola", "cola-a")
                        .param("exchange", "ex-a")
                        .param("routingKey", "a.b"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<BindingRequestDto> captor = ArgumentCaptor.forClass(BindingRequestDto.class);
        verify(service).eliminarBinding(captor.capture());
        assertEquals("cola-a", captor.getValue().getCola());
        assertEquals("ex-a", captor.getValue().getExchange());
        assertEquals("a.b", captor.getValue().getRoutingKey());
    }

    @Test
    void eliminarBinding_sinParametros_retorna400() throws Exception {
        mockMvc.perform(delete("/api/rabbitmq/bindings"))
                .andExpect(status().isBadRequest());

        verify(service, never()).eliminarBinding(any());
    }

    @Test
    void eliminarBinding_routingKeyInvalida_retorna400() throws Exception {
        mockMvc.perform(delete("/api/rabbitmq/bindings")
                        .param("cola", "cola-a")
                        .param("exchange", "ex-a")
                        .param("routingKey", "mala clave!"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarBindings_retornaLaLista() throws Exception {
        when(service.listarBindings()).thenReturn(List.of(Map.of("exchange", "ex-a", "routingKey", "a.b")));

        mockMvc.perform(get("/api/rabbitmq/bindings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routingKey").value("a.b"));
    }

    @Test
    void dtoSinDeadLetter_esConsistente() {
        QueueRequestDto dto = new QueueRequestDto();
        dto.setNombre("c");
        assertEquals(true, dto.isDeadLetterRoutingKeyConsistente());
        assertNull(dto.getTtlMs());
    }
}
