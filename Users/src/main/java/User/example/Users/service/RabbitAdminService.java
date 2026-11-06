package User.example.Users.service;

import User.example.Users.dto.BindingRequestDto;
import User.example.Users.dto.ExchangeRequestDto;
import User.example.Users.dto.QueueRequestDto;
import User.example.Users.exception.RabbitAdminException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administración de colas, exchanges y bindings de RabbitMQ.
 * Crear/eliminar usa AmqpAdmin (protocolo AMQP); listar usa la API HTTP del plugin de
 * management, porque AMQP no tiene operaciones de listado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final String PREFIJO_RESERVADO = "amq.";
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_DE_MAPAS =
            new ParameterizedTypeReference<>() {};

    private final AmqpAdmin amqpAdmin;
    private final RestTemplate restTemplate;

    // Colas/exchanges que usa la propia aplicación: se pueden consultar pero no eliminar.
    @Value("${rabbitmq.admin.protected-names:}")
    private String protectedNames;

    @Value("${rabbitmq.admin.management-url:http://localhost:15672}")
    private String managementUrl;

    @Value("${rabbitmq.admin.vhost:/}")
    private String vhost;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    // ── Colas ────────────────────────────────────────────────────────────────

    public void crearQueue(QueueRequestDto dto) {
        validarNoReservado(dto.getNombre());

        QueueBuilder builder = dto.isDurable()
                ? QueueBuilder.durable(dto.getNombre())
                : QueueBuilder.nonDurable(dto.getNombre());
        if (tieneTexto(dto.getDeadLetterExchange())) {
            builder.deadLetterExchange(dto.getDeadLetterExchange());
        }
        if (tieneTexto(dto.getDeadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(dto.getDeadLetterRoutingKey());
        }
        if (dto.getTtlMs() != null) {
            builder.ttl(dto.getTtlMs());
        }

        amqpAdmin.declareQueue(builder.build());
        log.info("[RabbitAdmin] Cola creada: {} (durable={}, dlx={}, ttlMs={})",
                dto.getNombre(), dto.isDurable(), dto.getDeadLetterExchange(), dto.getTtlMs());
    }

    public void eliminarQueue(String nombre) {
        validarNoReservado(nombre);
        validarNoProtegido(nombre);
        if (amqpAdmin.getQueueProperties(nombre) == null) {
            throw new EntityNotFoundException("Cola no encontrada: " + nombre);
        }
        amqpAdmin.deleteQueue(nombre);
        log.info("[RabbitAdmin] Cola eliminada: {}", nombre);
    }

    // ── Exchanges ────────────────────────────────────────────────────────────

    public void crearExchange(ExchangeRequestDto dto) {
        validarNoReservado(dto.getNombre());
        Exchange exchange = switch (dto.getTipo()) {
            case "direct" -> new DirectExchange(dto.getNombre(), dto.isDurable(), false);
            case "fanout" -> new FanoutExchange(dto.getNombre(), dto.isDurable(), false);
            default -> new TopicExchange(dto.getNombre(), dto.isDurable(), false);
        };
        amqpAdmin.declareExchange(exchange);
        log.info("[RabbitAdmin] Exchange creado: {} (tipo={}, durable={})", dto.getNombre(), dto.getTipo(), dto.isDurable());
    }

    public void eliminarExchange(String nombre) {
        validarNoReservado(nombre);
        validarNoProtegido(nombre);
        boolean eliminado = amqpAdmin.deleteExchange(nombre);
        log.info("[RabbitAdmin] Exchange eliminado: {} (existía={})", nombre, eliminado);
        if (!eliminado) {
            throw new EntityNotFoundException("Exchange no encontrado: " + nombre);
        }
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    public void crearBinding(BindingRequestDto dto) {
        amqpAdmin.declareBinding(construirBinding(dto));
        log.info("[RabbitAdmin] Binding creado: cola={} ← exchange={} (routingKey={})",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());
    }

    public void eliminarBinding(BindingRequestDto dto) {
        validarNoProtegido(dto.getCola());
        validarNoProtegido(dto.getExchange());
        amqpAdmin.removeBinding(construirBinding(dto));
        log.info("[RabbitAdmin] Binding eliminado: cola={} ← exchange={} (routingKey={})",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());
    }

    private Binding construirBinding(BindingRequestDto dto) {
        return BindingBuilder
                .bind(new Queue(dto.getCola()))
                .to(new TopicExchange(dto.getExchange()))
                .with(dto.getRoutingKey());
    }

    // ── Listados (API HTTP de management) ────────────────────────────────────

    public List<Map<String, Object>> listarQueues() {
        return consultar("queues").stream().map(this::resumenQueue).toList();
    }

    public List<Map<String, Object>> listarExchanges() {
        return consultar("exchanges").stream()
                .filter(e -> esNombreVisible(texto(e.get("name"))))
                .map(this::resumenExchange)
                .toList();
    }

    public List<Map<String, Object>> listarBindings() {
        return consultar("bindings").stream()
                .filter(b -> !texto(b.get("source")).isBlank())
                .map(this::resumenBinding)
                .toList();
    }

    private List<Map<String, Object>> consultar(String recurso) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        URI uri = URI.create(managementUrl.replaceAll("/+$", "") + "/api/" + recurso + "/"
                + URLEncoder.encode(vhost, StandardCharsets.UTF_8));
        try {
            ResponseEntity<List<Map<String, Object>>> respuesta =
                    restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), LISTA_DE_MAPAS);
            List<Map<String, Object>> cuerpo = respuesta.getBody();
            return cuerpo != null ? cuerpo : List.of();
        } catch (RestClientException e) {
            log.error("[RabbitAdmin] No se pudo consultar {}: {}", uri, e.getMessage());
            throw new RabbitAdminException("No se pudo consultar la API de administración de RabbitMQ", e);
        }
    }

    private Map<String, Object> resumenQueue(Map<String, Object> q) {
        String nombre = texto(q.get("name"));
        Object argumentos = q.get("arguments");
        Object dlx = argumentos instanceof Map<?, ?> mapa ? mapa.get("x-dead-letter-exchange") : null;

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("nombre", nombre);
        resumen.put("durable", q.get("durable"));
        resumen.put("autoDelete", q.get("auto_delete"));
        resumen.put("mensajes", q.get("messages"));
        resumen.put("consumidores", q.get("consumers"));
        resumen.put("deadLetterExchange", dlx);
        resumen.put("protegida", esProtegido(nombre));
        return resumen;
    }

    private Map<String, Object> resumenExchange(Map<String, Object> e) {
        String nombre = texto(e.get("name"));
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("nombre", nombre);
        resumen.put("tipo", e.get("type"));
        resumen.put("durable", e.get("durable"));
        resumen.put("autoDelete", e.get("auto_delete"));
        resumen.put("protegido", esProtegido(nombre));
        return resumen;
    }

    private Map<String, Object> resumenBinding(Map<String, Object> b) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("exchange", b.get("source"));
        resumen.put("destino", b.get("destination"));
        resumen.put("tipoDestino", b.get("destination_type"));
        resumen.put("routingKey", b.get("routing_key"));
        return resumen;
    }

    // ── Validaciones ─────────────────────────────────────────────────────────

    private void validarNoReservado(String nombre) {
        if (nombre != null && nombre.startsWith(PREFIJO_RESERVADO)) {
            throw new IllegalStateException("Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
    }

    private void validarNoProtegido(String nombre) {
        if (esProtegido(nombre)) {
            throw new IllegalStateException(
                    "'" + nombre + "' es un recurso que usa la propia aplicación y no se puede eliminar");
        }
    }

    private boolean esProtegido(String nombre) {
        Set<String> protegidos = Arrays.stream(protectedNames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return nombre != null && protegidos.contains(nombre);
    }

    private boolean esNombreVisible(String nombre) {
        return !nombre.isBlank() && !nombre.startsWith(PREFIJO_RESERVADO);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String texto(Object valor) {
        return valor != null ? valor.toString() : "";
    }
}
