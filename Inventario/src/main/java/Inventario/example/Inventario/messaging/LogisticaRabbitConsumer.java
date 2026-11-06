package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.OrdenCreadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoCreadoRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import Inventario.example.Inventario.service.LogisticaService;
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
 * Consumidor de las tareas de logística (ACK manual).
 *
 * Política de errores, igual para los dos listeners:
 *  - mensaje inválido            → NACK sin reencolar → va a la DLQ (no tiene sentido reintentarlo)
 *  - fallo recuperable (1ª vez)  → NACK con reencolado, un reintento
 *  - fallo tras el reintento     → NACK sin reencolar → va a la DLQ
 *  - éxito                       → ACK, siempre DESPUÉS de confirmar la transacción de base de datos
 *                                  (LogisticaService) y de publicar el resultado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitConsumer {

    private final LogisticaService logisticaService;
    private final LogisticaRabbitProducer logisticaRabbitProducer;

    // ── Dominio productos: ubicación física del producto ─────────────────────

    @RabbitListener(queues = "${rabbitmq.logistica.producto-creado.queue}")
    public void asignarUbicacionProducto(@Payload ProductoCreadoRabbitEvent evento,
                                          Message mensaje,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Object id = evento != null ? evento.getProductoId() : null;
        try {
            if (id == null) {
                throw new MensajeInvalidoException("productoId vacío");
            }
            ProductoUbicacionAsignadaRabbitEvent resultado =
                    logisticaService.asignarUbicacion(evento.getProductoId(), evento.getNombre());
            logisticaRabbitProducer.publicarUbicacionAsignada(resultado);

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Inventario RABBIT] producto.creado invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable("producto.creado", id, mensaje, channel, deliveryTag, e);
        }
    }

    // ── Dominio órdenes: bodega de despacho ──────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.logistica.orden-creada.queue}")
    public void asignarBodegaDespacho(@Payload OrdenCreadaRabbitEvent evento,
                                       Message mensaje,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Object id = evento != null ? evento.getOrdenId() : null;
        try {
            if (id == null) {
                throw new MensajeInvalidoException("ordenId vacío");
            }
            OrdenBodegaAsignadaRabbitEvent resultado = logisticaService.elegirBodegaDespacho(evento.getOrdenId());
            logisticaRabbitProducer.publicarBodegaAsignada(resultado);

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Inventario RABBIT] orden.creada invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable("orden.creada", id, mensaje, channel, deliveryTag, e);
        }
    }

    private void resolverFalloRecuperable(String evento, Object id, Message mensaje, Channel channel,
                                           long deliveryTag, Exception causa) throws IOException {
        boolean yaFueReintentado = mensaje.getMessageProperties().isRedelivered();
        if (yaFueReintentado) {
            log.error("[Inventario RABBIT] {} fallo tras reintento (id={}), a DLQ: {}", evento, id, causa.getMessage(), causa);
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.warn("[Inventario RABBIT] {} fallo recuperable (id={}), reencolando para un reintento: {}", evento, id, causa.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    public static class MensajeInvalidoException extends RuntimeException {
        public MensajeInvalidoException(String message) { super(message); }
    }
}
