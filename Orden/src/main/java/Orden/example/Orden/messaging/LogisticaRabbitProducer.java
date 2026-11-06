package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.OrdenCreadaRabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Productor RabbitMQ (EP3 DSY1107) — separado de OrdenEventProducer (Kafka); dispara
// la tarea asincrona de asignacion de bodega de despacho que resuelve Inventario.
@Slf4j
@Component
public class LogisticaRabbitProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.orden-creada.routing-key}")
    private String routingKey;

    public void publicarOrdenCreada(OrdenCreadaRabbitEvent evento) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
        log.info("[Orden RABBIT PRODUCER] orden.creada → exchange={} routingKey={} ordenId={}",
                exchangeName, routingKey, evento.getOrdenId());
    }
}
