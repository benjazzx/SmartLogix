package User.example.Users.messaging;

import User.example.Users.dto.UsuarioRegistradoRabbitEvent;
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
public class UsuarioRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.usuarios.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.usuarios.registrado.routing-key}")
    private String routingKey;

    // Si hay una transacción activa, el mensaje sale recién después del commit: así nunca se
    // anuncia un usuario que luego se revierte. Si RabbitMQ falla, el registro NO se aborta
    // (es una tarea de onboarding secundaria): se deja registro en el log.
    public void publicarUsuarioRegistrado(UsuarioRegistradoRabbitEvent evento) {
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

    private void enviar(UsuarioRegistradoRabbitEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
            log.info("[Users RABBIT PRODUCER] usuario.registrado → exchange={} routingKey={} userId={}",
                    exchangeName, routingKey, evento.getUserId());
        } catch (AmqpException e) {
            log.error("[Users RABBIT PRODUCER] no se pudo publicar usuario.registrado userId={}: {}",
                    evento.getUserId(), e.getMessage());
        }
    }
}
