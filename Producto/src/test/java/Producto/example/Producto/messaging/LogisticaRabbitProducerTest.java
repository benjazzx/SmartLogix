package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoCreadoRabbitEvent;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogisticaRabbitProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private LogisticaRabbitProducer producer;

    @BeforeEach
    void setUp() {
        producer = new LogisticaRabbitProducer(rabbitTemplate);
        ReflectionTestUtils.setField(producer, "exchangeName", "logistica-exchange");
        ReflectionTestUtils.setField(producer, "routingKey", "producto.creado");
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sinTransaccion_publicaDeInmediato() {
        ProductoCreadoRabbitEvent evento = new ProductoCreadoRabbitEvent(UUID.randomUUID(), "Casco");

        producer.publicarProductoCreado(evento);

        verify(rabbitTemplate).convertAndSend("logistica-exchange", "producto.creado", evento);
    }

    @Test
    void conTransaccion_soloPublicaDespuesDelCommit() {
        ProductoCreadoRabbitEvent evento = new ProductoCreadoRabbitEvent(UUID.randomUUID(), "Casco");
        TransactionSynchronizationManager.initSynchronization();

        producer.publicarProductoCreado(evento);

        verify(rabbitTemplate, never()).convertAndSend(eq("logistica-exchange"), eq("producto.creado"), any(Object.class));
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(rabbitTemplate).convertAndSend("logistica-exchange", "producto.creado", evento);
    }

    @Test
    void siRabbitFalla_laCreacionDelProductoNoSeAborta() {
        ProductoCreadoRabbitEvent evento = new ProductoCreadoRabbitEvent(UUID.randomUUID(), "Casco");
        doThrow(new AmqpIOException(new IOException("broker caído")))
                .when(rabbitTemplate).convertAndSend("logistica-exchange", "producto.creado", evento);

        assertDoesNotThrow(() -> producer.publicarProductoCreado(evento));
    }
}
