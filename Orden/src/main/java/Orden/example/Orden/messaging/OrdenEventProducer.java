package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.EstadoOrdenEvent;
import Orden.example.Orden.dto.OrdenCreadaEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class OrdenEventProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void publishOrdenCreada(OrdenCreadaEvent event) {
        streamBridge.send("orden-creada-topic", event);
        System.out.println("[Orden PRODUCER] Evento publicado en orden-creada-topic para orden: " + event.getOrdenId());
    }

    public void publishEstadoOrden(EstadoOrdenEvent event) {
        streamBridge.send("estado-orden-topic", event);
        System.out.println("[Orden PRODUCER] Evento publicado en estado-orden-topic: orden=" + event.getOrdenId() + " estado=" + event.getEstadoNombre());
    }
}
