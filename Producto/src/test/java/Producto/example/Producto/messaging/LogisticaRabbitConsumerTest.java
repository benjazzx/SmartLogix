package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoUbicacionAsignadaRabbitEvent;
import Producto.example.Producto.service.ProductoService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticaRabbitConsumerTest {

    private static final long TAG = 3L;

    @Mock private ProductoService productoService;
    @Mock private Channel channel;

    private LogisticaRabbitConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LogisticaRabbitConsumer(productoService);
    }

    private Message mensaje(boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setRedelivered(redelivered);
        return new Message(new byte[0], props);
    }

    @Test
    void mensajeValido_aplicaLaUbicacionYHaceAckDespuesDeLaTransaccion() throws IOException {
        UUID id = UUID.randomUUID();
        when(productoService.asignarUbicacionAutomatica(id, 1L, 2L, 3L)).thenReturn(true);

        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L), mensaje(false), channel, TAG);

        InOrder orden = inOrder(productoService, channel);
        orden.verify(productoService).asignarUbicacionAutomatica(id, 1L, 2L, 3L);
        orden.verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void productoQueYaTeniaUbicacion_tambienHaceAck() throws IOException {
        UUID id = UUID.randomUUID();
        when(productoService.asignarUbicacionAutomatica(id, 1L, 2L, 3L)).thenReturn(false);

        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L), mensaje(false), channel, TAG);

        verify(channel).basicAck(TAG, false);
    }

    @Test
    void sinProductoId_vaADlqSinReintentar() throws IOException {
        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(null, 1L, 2L, 3L), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(productoService);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void ubicacionIncompleta_vaADlqSinReintentar() throws IOException {
        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(UUID.randomUUID(), 1L, null, 3L), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(productoService);
    }

    @Test
    void eventoNulo_vaADlqSinReintentar() throws IOException {
        consumer.registrarUbicacionAsignada(null, mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(productoService);
    }

    @Test
    void productoAunNoExiste_primeraVezSeReencola() throws IOException {
        UUID id = UUID.randomUUID();
        when(productoService.asignarUbicacionAutomatica(id, 1L, 2L, 3L)).thenThrow(new RuntimeException("Producto no encontrado: " + id));

        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void productoNoExisteTrasElReintento_vaADlq() throws IOException {
        UUID id = UUID.randomUUID();
        when(productoService.asignarUbicacionAutomatica(id, 1L, 2L, 3L)).thenThrow(new RuntimeException("Producto no encontrado: " + id));

        consumer.registrarUbicacionAsignada(new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L), mensaje(true), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
