package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoUbicacionAsignadaRabbitEvent;
import Producto.example.Producto.service.ProductoService;
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
 * Consumidor de logística en Producto (ACK manual): guarda la ubicación que asignó Inventario.
 *
 *  - mensaje inválido            → NACK sin reencolar → DLQ
 *  - fallo recuperable (1ª vez)  → NACK con reencolado, un reintento (p. ej. el producto aún no se confirmó)
 *  - fallo tras el reintento     → NACK sin reencolar → DLQ
 *  - éxito                       → ACK, después de confirmar la transacción en ProductoService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitConsumer {

    private final ProductoService productoService;

    @RabbitListener(queues = "${rabbitmq.logistica.producto-ubicacion-asignada.queue}")
    public void registrarUbicacionAsignada(@Payload ProductoUbicacionAsignadaRabbitEvent evento,
                                            Message mensaje,
                                            Channel channel,
                                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Object id = evento != null ? evento.getProductoId() : null;
        try {
            if (id == null) {
                throw new MensajeInvalidoException("productoId vacío");
            }
            if (evento.getIdBodega() == null || evento.getIdPasillo() == null || evento.getIdEstante() == null) {
                throw new MensajeInvalidoException("ubicación incompleta (bodega, pasillo y estante son obligatorios)");
            }
            boolean asignada = productoService.asignarUbicacionAutomatica(
                    evento.getProductoId(), evento.getIdBodega(), evento.getIdPasillo(), evento.getIdEstante());
            log.info("[Producto RABBIT] producto.ubicacion-asignada → productoId={} aplicada={}", id, asignada);

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Producto RABBIT] producto.ubicacion-asignada invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable(id, mensaje, channel, deliveryTag, e);
        }
    }

    private void resolverFalloRecuperable(Object id, Message mensaje, Channel channel,
                                           long deliveryTag, Exception causa) throws IOException {
        if (mensaje.getMessageProperties().isRedelivered()) {
            log.error("[Producto RABBIT] producto.ubicacion-asignada fallo tras reintento (productoId={}), a DLQ: {}", id, causa.getMessage(), causa);
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.warn("[Producto RABBIT] producto.ubicacion-asignada fallo recuperable (productoId={}), reencolando para un reintento: {}", id, causa.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    public static class MensajeInvalidoException extends RuntimeException {
        public MensajeInvalidoException(String message) { super(message); }
    }
}
