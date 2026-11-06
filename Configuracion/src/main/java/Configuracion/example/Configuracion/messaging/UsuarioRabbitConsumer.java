package Configuracion.example.Configuracion.messaging;

import Configuracion.example.Configuracion.dto.UsuarioRegistradoRabbitEvent;
import Configuracion.example.Configuracion.service.ConfiguracionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de eventos de usuarios (ACK manual): cuando Users registra una cuenta, deja
 * creado su espacio de preferencias con la marca de onboarding pendiente.
 *
 *  - mensaje inválido            → NACK sin reencolar → DLQ
 *  - fallo recuperable (1ª vez)  → NACK con reencolado, un reintento
 *  - fallo tras el reintento     → NACK sin reencolar → DLQ
 *  - éxito                       → ACK, después de confirmar la transacción en ConfiguracionService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioRabbitConsumer {

    static final String CLAVE_ONBOARDING = "onboarding";
    static final String VALOR_ONBOARDING_PENDIENTE = "pendiente";

    private final ConfiguracionService configuracionService;

    @RabbitListener(queues = "${rabbitmq.usuarios.registrado.queue}")
    public void prepararPreferencias(@Payload UsuarioRegistradoRabbitEvent evento,
                                      Message mensaje,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Object id = evento != null ? evento.getUserId() : null;
        try {
            if (id == null) {
                throw new MensajeInvalidoException("userId vacío");
            }
            boolean creada = configuracionService.crearPreferenciaInicial(
                    evento.getUserId(), CLAVE_ONBOARDING, VALOR_ONBOARDING_PENDIENTE);
            log.info("[Configuracion RABBIT] usuario.registrado → userId={} preferenciaCreada={}", id, creada);

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Configuracion RABBIT] usuario.registrado invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable(id, mensaje, channel, deliveryTag, e);
        }
    }

    private void resolverFalloRecuperable(Object id, Message mensaje, Channel channel,
                                           long deliveryTag, Exception causa) throws IOException {
        if (mensaje.getMessageProperties().isRedelivered()) {
            log.error("[Configuracion RABBIT] usuario.registrado fallo tras reintento (userId={}), a DLQ: {}", id, causa.getMessage(), causa);
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.warn("[Configuracion RABBIT] usuario.registrado fallo recuperable (userId={}), reencolando para un reintento: {}", id, causa.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    public static class MensajeInvalidoException extends RuntimeException {
        public MensajeInvalidoException(String message) { super(message); }
    }
}
