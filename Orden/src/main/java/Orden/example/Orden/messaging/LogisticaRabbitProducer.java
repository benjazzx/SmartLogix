package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.OrdenCreadaRabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class LogisticaRabbitProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.orden-creada.routing-key}")
    private String routingKey;

    // Con transacción activa, el mensaje sale recién después del commit: Inventario nunca
    // recibe una orden que luego se revierte. Si RabbitMQ falla, la orden NO se pierde ni se
    // aborta (la asignación de bodega es una tarea secundaria): se deja registro en el log.
    public void publicarOrdenCreada(OrdenCreadaRabbitEvent evento) {
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

    private void enviar(OrdenCreadaRabbitEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
            log.info("[Orden RABBIT PRODUCER] orden.creada → exchange={} routingKey={} ordenId={}",
                    exchangeName, routingKey, evento.getOrdenId());
        } catch (AmqpException e) {
            log.error("[Orden RABBIT PRODUCER] no se pudo publicar orden.creada ordenId={}: {}",
                    evento.getOrdenId(), e.getMessage());
        }
    }
}
