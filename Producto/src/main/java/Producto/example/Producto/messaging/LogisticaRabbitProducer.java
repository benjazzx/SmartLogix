package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoCreadoRabbitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
