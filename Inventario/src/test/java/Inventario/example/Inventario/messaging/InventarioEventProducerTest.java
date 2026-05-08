package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.BodegaActualizadaEvent;
import Inventario.example.Inventario.dto.UbicacionActualizadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioEventProducerTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private InventarioEventProducer inventarioEventProducer;

    @Test
    void publishUbicacionActualizada_enviaAlTopicoCorrecto() {
        UbicacionActualizadaEvent evento = new UbicacionActualizadaEvent(
                1L, 10L, "EST-001", 20L, "PAS-A1",
                100L, "Bodega Central", 35.0, true,
                "CREADA", LocalDateTime.now()
        );
        when(streamBridge.send(eq("ubicacion-actualizada-topic"), eq(evento))).thenReturn(true);

        inventarioEventProducer.publishUbicacionActualizada(evento);

        verify(streamBridge).send("ubicacion-actualizada-topic", evento);
    }

    @Test
    void publishBodegaActualizada_enviaAlTopicoCorrecto() {
        BodegaActualizadaEvent evento = new BodegaActualizadaEvent(
                100L, "Bodega Central", "Santiago", "Chile", true,
                "CREADA", LocalDateTime.now()
        );
        when(streamBridge.send(eq("bodega-actualizada-topic"), eq(evento))).thenReturn(true);

        inventarioEventProducer.publishBodegaActualizada(evento);

        verify(streamBridge).send("bodega-actualizada-topic", evento);
    }

    @Test
    void publishUbicacionActualizada_enviaSoloUnaVez() {
        UbicacionActualizadaEvent evento = new UbicacionActualizadaEvent(
                2L, 11L, "EST-002", 21L, "PAS-B1",
                100L, "Bodega Norte", 60.0, true,
                "ACTUALIZADA", LocalDateTime.now()
        );
        when(streamBridge.send(anyString(), any())).thenReturn(false);

        inventarioEventProducer.publishUbicacionActualizada(evento);

        verify(streamBridge, times(1)).send(anyString(), any());
    }
}
