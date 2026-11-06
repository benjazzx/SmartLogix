package Orden.example.Orden.messaging;

import Orden.example.Orden.dto.OrdenBodegaAsignadaRabbitEvent;
import Orden.example.Orden.service.OrdenService;
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LogisticaRabbitConsumerTest {

    private static final long TAG = 7L;

    @Mock private OrdenService ordenService;
    @Mock private Channel channel;

    private LogisticaRabbitConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LogisticaRabbitConsumer(ordenService);
    }

    private Message mensaje(boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setRedelivered(redelivered);
        return new Message(new byte[0], props);
    }

    @Test
    void mensajeValido_guardaLaBodegaYHaceAckDespuesDeLaTransaccion() throws IOException {
        consumer.registrarBodegaDespacho(new OrdenBodegaAsignadaRabbitEvent(5L, 1L, "Central"), mensaje(false), channel, TAG);

        InOrder orden = inOrder(ordenService, channel);
        orden.verify(ordenService).registrarBodegaDespacho(5L, "Central");
        orden.verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void sinOrdenId_vaADlqSinReintentar() throws IOException {
        consumer.registrarBodegaDespacho(new OrdenBodegaAsignadaRabbitEvent(null, 1L, "Central"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(ordenService);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void sinNombreDeBodega_vaADlqSinReintentar() throws IOException {
        consumer.registrarBodegaDespacho(new OrdenBodegaAsignadaRabbitEvent(5L, 1L, "  "), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(ordenService);
    }

    @Test
    void eventoNulo_vaADlqSinReintentar() throws IOException {
        consumer.registrarBodegaDespacho(null, mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(ordenService);
    }

    @Test
    void ordenAunNoExiste_primeraVezSeReencola() throws IOException {
        doThrow(new IllegalStateException("Orden no encontrada: 5")).when(ordenService).registrarBodegaDespacho(5L, "Central");

        consumer.registrarBodegaDespacho(new OrdenBodegaAsignadaRabbitEvent(5L, 1L, "Central"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void ordenNoExisteTrasElReintento_vaADlq() throws IOException {
        doThrow(new IllegalStateException("Orden no encontrada: 5")).when(ordenService).registrarBodegaDespacho(5L, "Central");

        consumer.registrarBodegaDespacho(new OrdenBodegaAsignadaRabbitEvent(5L, 1L, "Central"), mensaje(true), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
