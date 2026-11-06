package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogisticaRabbitProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private LogisticaRabbitProducer producer;

    @BeforeEach
    void setUp() {
        producer = new LogisticaRabbitProducer(rabbitTemplate);
        ReflectionTestUtils.setField(producer, "exchangeName", "logistica-exchange");
        ReflectionTestUtils.setField(producer, "ubicacionAsignadaRoutingKey", "producto.ubicacion-asignada");
        ReflectionTestUtils.setField(producer, "bodegaAsignadaRoutingKey", "orden.bodega-asignada");
    }

    @Test
    void publicarUbicacionAsignada_usaElExchangeYLaRoutingKeyCorrectos() {
        ProductoUbicacionAsignadaRabbitEvent evento =
                new ProductoUbicacionAsignadaRabbitEvent(UUID.randomUUID(), 1L, 2L, 3L);

        producer.publicarUbicacionAsignada(evento);

        verify(rabbitTemplate).convertAndSend("logistica-exchange", "producto.ubicacion-asignada", evento);
    }

    @Test
    void publicarBodegaAsignada_usaElExchangeYLaRoutingKeyCorrectos() {
        OrdenBodegaAsignadaRabbitEvent evento = new OrdenBodegaAsignadaRabbitEvent(9L, 1L, "Central");

        producer.publicarBodegaAsignada(evento);

        verify(rabbitTemplate).convertAndSend("logistica-exchange", "orden.bodega-asignada", evento);
    }
}
