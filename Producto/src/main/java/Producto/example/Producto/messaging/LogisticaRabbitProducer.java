package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoCreadoRabbitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.producto-creado.routing-key}")
    private String routingKey;

    // Con transacción activa, el mensaje sale recién después del commit: Inventario nunca
    // recibe un producto que luego se revierte. Si RabbitMQ falla, la creación del producto
    // NO se aborta (la asignación de ubicación es una tarea secundaria): se deja registro en el log.
    public void publicarProductoCreado(ProductoCreadoRabbitEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(evento);
                }
            });
        } else {
            enviar(evento);
        }
    }

    private void enviar(ProductoCreadoRabbitEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
            log.info("[Producto RABBIT PRODUCER] producto.creado → exchange={} routingKey={} productoId={}",
                    exchangeName, routingKey, evento.getProductoId());
        } catch (AmqpException e) {
            log.error("[Producto RABBIT PRODUCER] no se pudo publicar producto.creado productoId={}: {}",
                    evento.getProductoId(), e.getMessage());
        }
    }
}
