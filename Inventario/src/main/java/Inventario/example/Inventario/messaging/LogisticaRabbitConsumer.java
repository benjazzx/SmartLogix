package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenCreadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoCreadoRabbitEvent;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.EstPasiRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitConsumer {

    private final EstPasiRepository estPasiRepository;
    private final BodegaRepository bodegaRepository;

    @RabbitListener(queues = "${rabbitmq.logistica.producto-creado.queue}")
    @Transactional(rollbackFor = Exception.class)
    public void asignarUbicacionProducto(@Payload ProductoCreadoRabbitEvent evento,
                                          Message mensaje,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (evento.getProductoId() == null) {
                throw new MensajeInvalidoException("productoId vacío");
            }
            EstPasiModel destino = elegirMejorUbicacion();
            double pctAntes = destino.getOcupacionPct() != null ? destino.getOcupacionPct() : 0.0;
            destino.setOcupacionPct(Math.min(100.0, pctAntes + 1.0));
            estPasiRepository.save(destino);

            log.info("[Inventario RABBIT] Producto '{}' ({}) asignado — estante={} pasillo={} bodega={} ocupación {}%→{}%",
                    evento.getNombre(), evento.getProductoId(),
                    destino.getEstante().getCodigo(), destino.getPasillo().getIdPasillo(),
                    destino.getPasillo().getBodega().getNombre(), pctAntes, destino.getOcupacionPct());

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Inventario RABBIT] producto.creado invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable("producto.creado", evento.getProductoId(), mensaje, channel, deliveryTag, e);
        }
    }

    private EstPasiModel elegirMejorUbicacion() {
        List<EstPasiModel> habilitadas = estPasiRepository.findByHabilitadaTrue();
        return habilitadas.stream()
                .min(Comparator.comparing(ep -> ep.getOcupacionPct() != null ? ep.getOcupacionPct() : 0.0))
                .orElseThrow(() -> new SinCapacidadException("No hay ubicaciones habilitadas en ningún pasillo"));
    }

    @RabbitListener(queues = "${rabbitmq.logistica.orden-creada.queue}")
    public void asignarBodegaDespacho(@Payload OrdenCreadaRabbitEvent evento,
                                       Message mensaje,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            if (evento.getOrdenId() == null) {
                throw new MensajeInvalidoException("ordenId vacío");
            }
            BodegaModel bodega = elegirBodegaConMasCapacidad();

            log.info("[Inventario RABBIT] Orden #{} asignada para despacho desde bodega '{}' (id={})",
                    evento.getOrdenId(), bodega.getNombre(), bodega.getIdBodega());

            channel.basicAck(deliveryTag, false);

        } catch (MensajeInvalidoException e) {
            log.error("[Inventario RABBIT] orden.creada invalido, a DLQ sin reintentar: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            resolverFalloRecuperable("orden.creada", evento.getOrdenId(), mensaje, channel, deliveryTag, e);
        }
    }

    private BodegaModel elegirBodegaConMasCapacidad() {
        List<BodegaModel> activas = bodegaRepository.findByActivaTrue();
        if (activas.isEmpty()) {
            throw new SinCapacidadException("No hay bodegas activas");
        }
        return activas.stream()
                .min(Comparator.comparing(b -> {
                    Double prom = estPasiRepository.calcularOcupacionPromedioPorBodega(b.getIdBodega());
                    return prom != null ? prom : 0.0;
                }))
                .orElseThrow(() -> new SinCapacidadException("No hay bodegas activas"));
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

    public static class SinCapacidadException extends RuntimeException {
        public SinCapacidadException(String message) { super(message); }
    }
}
