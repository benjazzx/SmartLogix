package User.example.Users.service;

import User.example.Users.dto.BindingRequestDto;
import User.example.Users.dto.ExchangeRequestDto;
import User.example.Users.dto.QueueRequestDto;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public void crearQueue(QueueRequestDto dto) {
        Queue queue = QueueBuilder.durable(dto.getNombre()).build();
        if (!dto.isDurable()) {
            queue = QueueBuilder.nonDurable(dto.getNombre()).build();
        }
        amqpAdmin.declareQueue(queue);
        log.info("[RabbitAdmin] Cola creada: {} (durable={})", dto.getNombre(), dto.isDurable());
    }

    public void eliminarQueue(String nombre) {
        boolean eliminada = amqpAdmin.deleteQueue(nombre);
        log.info("[RabbitAdmin] Cola eliminada: {} (existía={})", nombre, eliminada);
        if (!eliminada) {
            throw new jakarta.persistence.EntityNotFoundException("Cola no encontrada: " + nombre);
        }
    }

    public void crearExchange(ExchangeRequestDto dto) {
        Exchange exchange = switch (dto.getTipo()) {
            case "direct" -> new DirectExchange(dto.getNombre(), dto.isDurable(), false);
            case "fanout" -> new FanoutExchange(dto.getNombre(), dto.isDurable(), false);
            default -> new TopicExchange(dto.getNombre(), dto.isDurable(), false);
        };
        amqpAdmin.declareExchange(exchange);
        log.info("[RabbitAdmin] Exchange creado: {} (tipo={}, durable={})", dto.getNombre(), dto.getTipo(), dto.isDurable());
    }

    public void eliminarExchange(String nombre) {
        boolean eliminado = amqpAdmin.deleteExchange(nombre);
        log.info("[RabbitAdmin] Exchange eliminado: {} (existía={})", nombre, eliminado);
        if (!eliminado) {
            throw new jakarta.persistence.EntityNotFoundException("Exchange no encontrado: " + nombre);
        }
    }

    public void crearBinding(BindingRequestDto dto) {
        Binding binding = construirBinding(dto);
        amqpAdmin.declareBinding(binding);
        log.info("[RabbitAdmin] Binding creado: cola={} ← exchange={} (routingKey={})",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());
    }

    public void eliminarBinding(BindingRequestDto dto) {
        Binding binding = construirBinding(dto);
        amqpAdmin.removeBinding(binding);
        log.info("[RabbitAdmin] Binding eliminado: cola={} ← exchange={} (routingKey={})",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());
    }

    private Binding construirBinding(BindingRequestDto dto) {
        return BindingBuilder
                .bind(new Queue(dto.getCola()))
                .to(new TopicExchange(dto.getExchange()))
                .with(dto.getRoutingKey());
    }
}
