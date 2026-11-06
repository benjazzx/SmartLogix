package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.OrdenBodegaAsignadaRabbitEvent;
import Orden.example.Orden.service.OrdenService;
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
 * Consumidor de logística en Orden (ACK manual): guarda la bodega de despacho que asignó Inventario.
 *
 *  - mensaje inválido            → NACK sin reencolar → DLQ
 *  - fallo recuperable (1ª vez)  → NACK con reencolado, un reintento (p. ej. la orden aún no se confirmó)
 *  - fallo tras el reintento     → NACK sin reencolar → DLQ
 *  - éxito                       → ACK, después de confirmar la transacción en OrdenService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitConsumer {

    private final OrdenService ordenService;

    @RabbitListener(queues = "${rabbitmq.logistica.orden-bodega-asignada.queue}")
    public void registrarBodegaDespacho(@Payload OrdenBodegaAsignadaRabbitEvent evento,
                                         Message mensaje,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Object id = evento != null ? evento.getOrdenId() : null;
        try {
            if (id == null) {
                throw new MensajeInvalidoException("ordenId vacío");
            }
            if (evento.getNombreBodega() == null || evento.getNombreBodega().isBlank()) {
                throw new MensajeInvalidoException("nombreBodega vacío");
            }
            ordenService.registrarBodegaDespacho(evento.getOrdenId(), evento.getNombreBodega());
            log.info("[Orden RABBIT] orden.bodega-asignada → ordenId={} bodega='{}'", id, evento.getNombreBodega());

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Orden RABBIT] orden.bodega-asignada invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable(id, mensaje, channel, deliveryTag, e);
        }
    }

    private void resolverFalloRecuperable(Object id, Message mensaje, Channel channel,
                                           long deliveryTag, Exception causa) throws IOException {
        if (mensaje.getMessageProperties().isRedelivered()) {
            log.error("[Orden RABBIT] orden.bodega-asignada fallo tras reintento (ordenId={}), a DLQ: {}", id, causa.getMessage(), causa);
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.warn("[Orden RABBIT] orden.bodega-asignada fallo recuperable (ordenId={}), reencolando para un reintento: {}", id, causa.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    public static class MensajeInvalidoException extends RuntimeException {
        public MensajeInvalidoException(String message) { super(message); }
    }
}
