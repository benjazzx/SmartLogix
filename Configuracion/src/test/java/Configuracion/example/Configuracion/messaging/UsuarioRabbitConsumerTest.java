package Configuracion.example.Configuracion.messaging;

import Configuracion.example.Configuracion.dto.UsuarioRegistradoRabbitEvent;
import Configuracion.example.Configuracion.service.ConfiguracionService;
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
class UsuarioRabbitConsumerTest {

    private static final long TAG = 11L;

    @Mock private ConfiguracionService configuracionService;
    @Mock private Channel channel;

    private UsuarioRabbitConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UsuarioRabbitConsumer(configuracionService);
    }

    private Message mensaje(boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setRedelivered(redelivered);
        return new Message(new byte[0], props);
    }

    @Test
    void mensajeValido_creaLaPreferenciaYHaceAckDespuesDeLaTransaccion() throws IOException {
        UUID userId = UUID.randomUUID();
        when(configuracionService.crearPreferenciaInicial(userId, "onboarding", "pendiente")).thenReturn(true);

        consumer.prepararPreferencias(new UsuarioRegistradoRabbitEvent(userId, "cliente"), mensaje(false), channel, TAG);

        InOrder orden = inOrder(configuracionService, channel);
        orden.verify(configuracionService).crearPreferenciaInicial(userId, "onboarding", "pendiente");
        orden.verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void preferenciaYaExistente_tambienHaceAck() throws IOException {
        UUID userId = UUID.randomUUID();
        when(configuracionService.crearPreferenciaInicial(userId, "onboarding", "pendiente")).thenReturn(false);

        consumer.prepararPreferencias(new UsuarioRegistradoRabbitEvent(userId, "cliente"), mensaje(false), channel, TAG);

        verify(channel).basicAck(TAG, false);
    }

    @Test
    void sinUserId_vaADlqSinReintentar() throws IOException {
        consumer.prepararPreferencias(new UsuarioRegistradoRabbitEvent(null, "cliente"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(configuracionService);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void eventoNulo_vaADlqSinReintentar() throws IOException {
        consumer.prepararPreferencias(null, mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoInteractions(configuracionService);
    }

    @Test
    void falloDeBaseDeDatos_primeraVezSeReencola() throws IOException {
        UUID userId = UUID.randomUUID();
        when(configuracionService.crearPreferenciaInicial(userId, "onboarding", "pendiente"))
                .thenThrow(new IllegalStateException("BD caída"));

        consumer.prepararPreferencias(new UsuarioRegistradoRabbitEvent(userId, "cliente"), mensaje(false), channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void falloDeBaseDeDatos_trasElReintentoVaADlq() throws IOException {
        UUID userId = UUID.randomUUID();
        when(configuracionService.crearPreferenciaInicial(userId, "onboarding", "pendiente"))
                .thenThrow(new IllegalStateException("BD caída"));

        consumer.prepararPreferencias(new UsuarioRegistradoRabbitEvent(userId, "cliente"), mensaje(true), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
