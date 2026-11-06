package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoCreadoRabbitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Productor RabbitMQ (EP3 DSY1107) — separado de ProductoEventProducer (Kafka), que
// publica eventos de dominio para otros servicios; este exclusivamente dispara la
// tarea asincrona de asignacion de ubicacion que resuelve Inventario.
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.producto-creado.routing-key}")
    private String routingKey;

    public void publicarProductoCreado(ProductoCreadoRabbitEvent evento) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
        log.info("[Producto RABBIT PRODUCER] producto.creado → exchange={} routingKey={} productoId={}",
                exchangeName, routingKey, evento.getProductoId());
    }
}
