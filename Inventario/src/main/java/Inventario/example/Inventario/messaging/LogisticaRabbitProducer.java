package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Inventario responde a los eventos de Producto y Orden publicando el resultado de la
// asignación en el mismo exchange de logística. Producto y Orden lo consumen.
@Slf4j
@Component
@RequiredArgsConstructor
public class LogisticaRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.producto-ubicacion-asignada.routing-key}")
    private String ubicacionAsignadaRoutingKey;

    @Value("${rabbitmq.logistica.orden-bodega-asignada.routing-key}")
    private String bodegaAsignadaRoutingKey;

    public void publicarUbicacionAsignada(ProductoUbicacionAsignadaRabbitEvent evento) {
        rabbitTemplate.convertAndSend(exchangeName, ubicacionAsignadaRoutingKey, evento);
        log.info("[Inventario RABBIT PRODUCER] producto.ubicacion-asignada → exchange={} routingKey={} productoId={}",
                exchangeName, ubicacionAsignadaRoutingKey, evento.getProductoId());
    }

    public void publicarBodegaAsignada(OrdenBodegaAsignadaRabbitEvent evento) {
        rabbitTemplate.convertAndSend(exchangeName, bodegaAsignadaRoutingKey, evento);
        log.info("[Inventario RABBIT PRODUCER] orden.bodega-asignada → exchange={} routingKey={} ordenId={}",
                exchangeName, bodegaAsignadaRoutingKey, evento.getOrdenId());
    }
}
