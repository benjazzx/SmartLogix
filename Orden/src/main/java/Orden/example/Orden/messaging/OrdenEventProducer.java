package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.EstadoOrdenEvent;
import Orden.example.Orden.dto.OrdenCreadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrdenEventProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void publishOrdenCreada(OrdenCreadaEvent event) {
        streamBridge.send("orden-creada-topic", event);
        log.info("[PRODUCER] orden-creada-topic → ordenId={}", event.getOrdenId());
    }

    public void publishEstadoOrden(EstadoOrdenEvent event) {
        streamBridge.send("estado-orden-topic", event);
        log.info("[PRODUCER] estado-orden-topic → ordenId={} estado={}", event.getOrdenId(), event.getEstadoNombre());
    }
}
