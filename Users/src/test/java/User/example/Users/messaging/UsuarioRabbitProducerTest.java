package User.example.Users.messaging;

import User.example.Users.dto.UsuarioRegistradoRabbitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuarioRabbitProducerTest {

    private static final String EXCHANGE = "usuarios-exchange";
    private static final String ROUTING_KEY = "usuario.registrado";

    @Mock private RabbitTemplate rabbitTemplate;

    private UsuarioRabbitProducer producer;

    @BeforeEach
    void setUp() {
        producer = new UsuarioRabbitProducer(rabbitTemplate);
        ReflectionTestUtils.setField(producer, "exchangeName", EXCHANGE);
        ReflectionTestUtils.setField(producer, "routingKey", ROUTING_KEY);
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sinTransaccion_publicaDeInmediato() {
        UsuarioRegistradoRabbitEvent evento = new UsuarioRegistradoRabbitEvent(UUID.randomUUID(), "cliente");

        producer.publicarUsuarioRegistrado(evento);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, evento);
    }

    @Test
    void conTransaccion_noPublicaHastaElCommit() {
        UsuarioRegistradoRabbitEvent evento = new UsuarioRegistradoRabbitEvent(UUID.randomUUID(), "cliente");
        TransactionSynchronizationManager.initSynchronization();

        producer.publicarUsuarioRegistrado(evento);

        verify(rabbitTemplate, never()).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class));

        List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, sincronizaciones.size());
        sincronizaciones.forEach(TransactionSynchronization::afterCommit);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, evento);
    }

    @Test
    void siRabbitFalla_noPropagaLaExcepcion() {
        UsuarioRegistradoRabbitEvent evento = new UsuarioRegistradoRabbitEvent(UUID.randomUUID(), "cliente");
        doThrow(new AmqpIOException(new IOException("broker caído")))
                .when(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, evento);

        assertDoesNotThrow(() -> producer.publicarUsuarioRegistrado(evento));
    }
}
