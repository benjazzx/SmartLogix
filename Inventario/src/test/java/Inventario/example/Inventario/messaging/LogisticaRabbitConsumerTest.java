package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.OrdenCreadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoCreadoRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import Inventario.example.Inventario.exception.SinCapacidadException;
import Inventario.example.Inventario.service.LogisticaService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticaRabbitConsumerTest {

    private static final long TAG = 42L;

    @Mock private LogisticaService logisticaService;
    @Mock private LogisticaRabbitProducer producer;
    @Mock private Channel channel;

    private LogisticaRabbitConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LogisticaRabbitConsumer(logisticaService, producer);
    }

    private Message mensaje(boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setRedelivered(redelivered);
        return new Message(new byte[0], props);
    }

    // ── producto.creado ──────────────────────────────────────────────────────

    @Test
    void producto_ok_publicaElResultadoYHaceAckDespuesDeLaTransaccion() throws IOException {
        UUID id = UUID.randomUUID();
        ProductoUbicacionAsignadaRabbitEvent resultado = new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L);
        when(logisticaService.asignarUbicacion(id, "Casco")).thenReturn(resultado);

        consumer.asignarUbicacionProducto(new ProductoCreadoRabbitEvent(id, "Casco"), mensaje(false), channel, TAG);

        // Orden estricto: 1) commit en el servicio, 2) publicar resultado, 3) ACK.
        InOrder orden = inOrder(logisticaService, producer, channel);
        orden.verify(logisticaService).asignarUbicacion(id, "Casco");
        orden.verify(producer).publicarUbicacionAsignada(resultado);
        orden.verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void producto_sinId_vaADlqSinReintentar() throws IOException {
        consumer.asignarUbicacionProducto(new ProductoCreadoRabbitEvent(null, "X"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(logisticaService, producer);
        verify(channel, never()).basicAck(anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void producto_eventoNulo_vaADlqSinReintentar() throws IOException {
        consumer.asignarUbicacionProducto(null, mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(logisticaService, producer);
    }

    @Test
    void producto_fallaLaPrimeraVez_seReencola() throws IOException {
        UUID id = UUID.randomUUID();
        when(logisticaService.asignarUbicacion(any(), anyString()))
                .thenThrow(new SinCapacidadException("sin ubicaciones"));

        consumer.asignarUbicacionProducto(new ProductoCreadoRabbitEvent(id, "X"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verify(channel, never()).basicAck(anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
        verifyNoInteractions(producer);
    }

    @Test
    void producto_fallaTrasElReintento_vaADlq() throws IOException {
        UUID id = UUID.randomUUID();
        when(logisticaService.asignarUbicacion(any(), anyString()))
                .thenThrow(new SinCapacidadException("sin ubicaciones"));

        consumer.asignarUbicacionProducto(new ProductoCreadoRabbitEvent(id, "X"), mensaje(true), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void producto_siPublicarElResultadoFalla_noHaceAckYReintenta() throws IOException {
        UUID id = UUID.randomUUID();
        ProductoUbicacionAsignadaRabbitEvent resultado = new ProductoUbicacionAsignadaRabbitEvent(id, 1L, 2L, 3L);
        when(logisticaService.asignarUbicacion(id, "X")).thenReturn(resultado);
        doThrow(new AmqpIOException(new IOException("broker caído"))).when(producer).publicarUbicacionAsignada(resultado);

        consumer.asignarUbicacionProducto(new ProductoCreadoRabbitEvent(id, "X"), mensaje(false), channel, TAG);

        verify(channel, never()).basicAck(anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(channel).basicNack(TAG, false, true);
    }

    // ── orden.creada ─────────────────────────────────────────────────────────

    @Test
    void orden_ok_publicaElResultadoYHaceAck() throws IOException {
        OrdenBodegaAsignadaRabbitEvent resultado = new OrdenBodegaAsignadaRabbitEvent(7L, 1L, "Central");
        when(logisticaService.elegirBodegaDespacho(7L)).thenReturn(resultado);

        consumer.asignarBodegaDespacho(new OrdenCreadaRabbitEvent(7L, UUID.randomUUID()), mensaje(false), channel, TAG);

        InOrder orden = inOrder(logisticaService, producer, channel);
        orden.verify(logisticaService).elegirBodegaDespacho(7L);
        orden.verify(producer).publicarBodegaAsignada(resultado);
        orden.verify(channel).basicAck(TAG, false);
    }

    @Test
    void orden_sinId_vaADlqSinReintentar() throws IOException {
        consumer.asignarBodegaDespacho(new OrdenCreadaRabbitEvent(null, UUID.randomUUID()), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(logisticaService, producer);
    }

    @Test
    void orden_sinBodegasActivas_primeraVezSeReencola() throws IOException {
        when(logisticaService.elegirBodegaDespacho(7L)).thenThrow(new SinCapacidadException("No hay bodegas activas"));

        consumer.asignarBodegaDespacho(new OrdenCreadaRabbitEvent(7L, UUID.randomUUID()), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, true);
    }

    @Test
    void orden_sinBodegasActivas_trasElReintentoVaADlq() throws IOException {
        when(logisticaService.elegirBodegaDespacho(7L)).thenThrow(new SinCapacidadException("No hay bodegas activas"));

        consumer.asignarBodegaDespacho(new OrdenCreadaRabbitEvent(7L, UUID.randomUUID()), mensaje(true), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
    }

    @Test
    void orden_eventoNulo_vaADlq() throws IOException {
        consumer.asignarBodegaDespacho(null, mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
    }
}
