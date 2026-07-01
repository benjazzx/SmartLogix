package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.EstadoOrdenEvent;
import Orden.example.Orden.dto.OrdenCreadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenEventProducerTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private OrdenEventProducer ordenEventProducer;

    @Test
    void publishOrdenCreada_enviaAlTopicCorrecto() {
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                1L, UUID.randomUUID(), "Juan Pérez",
                UUID.randomUUID(), LocalDateTime.now(), List.of()
        );

        ordenEventProducer.publishOrdenCreada(event);

        verify(streamBridge).send(eq("orden-creada-topic"), eq(event));
    }

    @Test
    void publishOrdenCreada_eventoConDatosCorrecto() {
        UUID userId = UUID.randomUUID();
        OrdenCreadaEvent event = new OrdenCreadaEvent(
                42L, userId, "María López",
                UUID.randomUUID(), LocalDateTime.now(), List.of()
        );

        ordenEventProducer.publishOrdenCreada(event);

        ArgumentCaptor<OrdenCreadaEvent> captor = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(streamBridge).send(eq("orden-creada-topic"), captor.capture());
        assertEquals(42L, captor.getValue().getOrdenId());
        assertEquals(userId, captor.getValue().getUserId());
    }

    @Test
    void publishEstadoOrden_enviaAlTopicCorrecto() {
        EstadoOrdenEvent event = new EstadoOrdenEvent(
                1L, UUID.randomUUID(), UUID.randomUUID(),
                "en_proceso", "Procesando", LocalDateTime.now()
        );

        ordenEventProducer.publishEstadoOrden(event);

        verify(streamBridge).send(eq("estado-orden-topic"), eq(event));
    }

    @Test
    void publishEstadoOrden_eventoConDatosCorrecto() {
        UUID estadoId = UUID.randomUUID();
        EstadoOrdenEvent event = new EstadoOrdenEvent(
                99L, UUID.randomUUID(), estadoId,
                "entregado", "Orden entregada", LocalDateTime.now()
        );

        ordenEventProducer.publishEstadoOrden(event);

        ArgumentCaptor<EstadoOrdenEvent> captor = ArgumentCaptor.forClass(EstadoOrdenEvent.class);
        verify(streamBridge).send(eq("estado-orden-topic"), captor.capture());
        assertEquals("entregado", captor.getValue().getEstadoNombre());
        assertEquals(estadoId, captor.getValue().getEstadoId());
    }
}
