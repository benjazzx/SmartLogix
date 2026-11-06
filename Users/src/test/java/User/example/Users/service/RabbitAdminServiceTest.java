package User.example.Users.service;

import User.example.Users.dto.BindingRequestDto;
import User.example.Users.dto.ExchangeRequestDto;
import User.example.Users.dto.QueueRequestDto;
import User.example.Users.exception.RabbitAdminException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock private AmqpAdmin amqpAdmin;
    @Mock private RestTemplate restTemplate;

    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        service = new RabbitAdminService(amqpAdmin, restTemplate);
        ReflectionTestUtils.setField(service, "protectedNames", "logistica-exchange, orden-bodega-queue");
        ReflectionTestUtils.setField(service, "managementUrl", "http://rabbitmq:15672/");
        ReflectionTestUtils.setField(service, "vhost", "/");
        ReflectionTestUtils.setField(service, "username", "smartlogix");
        ReflectionTestUtils.setField(service, "password", "secreto");
    }

    private QueueRequestDto queueDto(String nombre) {
        QueueRequestDto dto = new QueueRequestDto();
        dto.setNombre(nombre);
        return dto;
    }

    private BindingRequestDto bindingDto(String cola, String exchange, String rk) {
        BindingRequestDto dto = new BindingRequestDto();
        dto.setCola(cola);
        dto.setExchange(exchange);
        dto.setRoutingKey(rk);
        return dto;
    }

    // ── Colas ────────────────────────────────────────────────────────────────

    @Test
    void crearQueue_durableSimple_declaraLaCola() {
        service.crearQueue(queueDto("cola-nueva"));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertEquals("cola-nueva", captor.getValue().getName());
        assertTrue(captor.getValue().isDurable());
        assertFalse(captor.getValue().getArguments().containsKey("x-dead-letter-exchange"));
    }

    @Test
    void crearQueue_noDurable_declaraColaNoDurable() {
        QueueRequestDto dto = queueDto("cola-temporal");
        dto.setDurable(false);

        service.crearQueue(dto);

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertFalse(captor.getValue().isDurable());
    }

    @Test
    void crearQueue_conDeadLetterYTtl_agregaLosArgumentos() {
        QueueRequestDto dto = queueDto("cola-con-dlq");
        dto.setDeadLetterExchange("logistica-dlx");
        dto.setDeadLetterRoutingKey("cola.dlq");
        dto.setTtlMs(60_000);

        service.crearQueue(dto);

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Map<String, Object> args = captor.getValue().getArguments();
        assertEquals("logistica-dlx", args.get("x-dead-letter-exchange"));
        assertEquals("cola.dlq", args.get("x-dead-letter-routing-key"));
        assertEquals(60_000, args.get("x-message-ttl"));
    }

    @Test
    void crearQueue_nombreReservado_lanzaExcepcion() {
        assertThrows(IllegalStateException.class, () -> service.crearQueue(queueDto("amq.gen-123")));

        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void eliminarQueue_existente_laElimina() {
        when(amqpAdmin.getQueueProperties("cola-vieja")).thenReturn(new Properties());

        service.eliminarQueue("cola-vieja");

        verify(amqpAdmin).deleteQueue("cola-vieja");
    }

    @Test
    void eliminarQueue_noExiste_lanzaNotFound() {
        when(amqpAdmin.getQueueProperties("fantasma")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.eliminarQueue("fantasma"));

        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarQueue_protegida_noSeElimina() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.eliminarQueue("orden-bodega-queue"));

        assertTrue(ex.getMessage().contains("orden-bodega-queue"));
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarQueue_reservada_noSeElimina() {
        assertThrows(IllegalStateException.class, () -> service.eliminarQueue("amq.rabbitmq.reply-to"));

        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    // ── Exchanges ────────────────────────────────────────────────────────────

    @Test
    void crearExchange_segunElTipo() {
        for (String tipo : List.of("direct", "fanout", "topic")) {
            ExchangeRequestDto dto = new ExchangeRequestDto();
            dto.setNombre("ex-" + tipo);
            dto.setTipo(tipo);
            service.crearExchange(dto);
        }

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin, org.mockito.Mockito.times(3)).declareExchange(captor.capture());
        List<Exchange> declarados = captor.getAllValues();
        assertInstanceOf(DirectExchange.class, declarados.get(0));
        assertInstanceOf(FanoutExchange.class, declarados.get(1));
        assertInstanceOf(TopicExchange.class, declarados.get(2));
    }

    @Test
    void eliminarExchange_existente_loElimina() {
        when(amqpAdmin.deleteExchange("ex-viejo")).thenReturn(true);

        service.eliminarExchange("ex-viejo");

        verify(amqpAdmin).deleteExchange("ex-viejo");
    }

    @Test
    void eliminarExchange_noExiste_lanzaNotFound() {
        when(amqpAdmin.deleteExchange("ex-fantasma")).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> service.eliminarExchange("ex-fantasma"));
    }

    @Test
    void eliminarExchange_protegido_noSeElimina() {
        assertThrows(IllegalStateException.class, () -> service.eliminarExchange("logistica-exchange"));

        verify(amqpAdmin, never()).deleteExchange(anyString());
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Test
    void crearBinding_declaraElBinding() {
        service.crearBinding(bindingDto("cola-a", "ex-a", "a.b"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertEquals("cola-a", captor.getValue().getDestination());
        assertEquals("ex-a", captor.getValue().getExchange());
        assertEquals("a.b", captor.getValue().getRoutingKey());
    }

    @Test
    void eliminarBinding_libre_loElimina() {
        service.eliminarBinding(bindingDto("cola-a", "ex-a", "a.b"));

        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarBinding_deColaProtegida_noSeElimina() {
        assertThrows(IllegalStateException.class,
                () -> service.eliminarBinding(bindingDto("orden-bodega-queue", "ex-a", "x")));

        verify(amqpAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarBinding_deExchangeProtegido_noSeElimina() {
        assertThrows(IllegalStateException.class,
                () -> service.eliminarBinding(bindingDto("cola-a", "logistica-exchange", "x")));

        verify(amqpAdmin, never()).removeBinding(any(Binding.class));
    }

    // ── Listados ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubManagement(List<Map<String, Object>> cuerpo) {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(cuerpo));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarQueues_resumeYMarcaLasProtegidas() {
        stubManagement(List.of(
                Map.of("name", "orden-bodega-queue", "durable", true, "auto_delete", false,
                        "messages", 3, "consumers", 1,
                        "arguments", Map.of("x-dead-letter-exchange", "logistica-dlx")),
                Map.of("name", "cola-libre", "durable", true, "auto_delete", false)));

        List<Map<String, Object>> resultado = service.listarQueues();

        assertEquals(2, resultado.size());
        assertEquals("orden-bodega-queue", resultado.get(0).get("nombre"));
        assertEquals(true, resultado.get(0).get("protegida"));
        assertEquals("logistica-dlx", resultado.get(0).get("deadLetterExchange"));
        assertEquals(false, resultado.get(1).get("protegida"));

        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.GET), entity.capture(),
                any(ParameterizedTypeReference.class));
        assertEquals("http://rabbitmq:15672/api/queues/%2F", uri.getValue().toString());
        assertNotNull(entity.getValue().getHeaders().getFirst("Authorization"));
        assertTrue(entity.getValue().getHeaders().getFirst("Authorization").startsWith("Basic "));
    }

    @Test
    void listarExchanges_omiteElDefaultYLosReservados() {
        stubManagement(List.of(
                Map.of("name", "", "type", "direct"),
                Map.of("name", "amq.topic", "type", "topic"),
                Map.of("name", "logistica-exchange", "type", "topic", "durable", true, "auto_delete", false)));

        List<Map<String, Object>> resultado = service.listarExchanges();

        assertEquals(1, resultado.size());
        assertEquals("logistica-exchange", resultado.get(0).get("nombre"));
        assertEquals(true, resultado.get(0).get("protegido"));
    }

    @Test
    void listarBindings_omiteLosDelExchangePorDefecto() {
        stubManagement(List.of(
                Map.of("source", "", "destination", "cola-a", "destination_type", "queue", "routing_key", "cola-a"),
                Map.of("source", "ex-a", "destination", "cola-a", "destination_type", "queue", "routing_key", "a.b")));

        List<Map<String, Object>> resultado = service.listarBindings();

        assertEquals(1, resultado.size());
        assertEquals("ex-a", resultado.get(0).get("exchange"));
        assertEquals("a.b", resultado.get(0).get("routingKey"));
    }

    @Test
    void listar_siLaApiDeManagementFalla_lanzaRabbitAdminException() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThrows(RabbitAdminException.class, () -> service.listarQueues());
    }
}
