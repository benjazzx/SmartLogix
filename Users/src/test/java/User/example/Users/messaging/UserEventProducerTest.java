package User.example.Users.messaging;

import User.example.Users.dto.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventProducerTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private UserEventProducer userEventProducer;

    @Test
    void publishUserCreated_enviaAlTopicoCorrecto() {
        UserCreatedEvent evento = new UserCreatedEvent(UUID.randomUUID(), "nuevo@test.cl", "cliente");

        userEventProducer.publishUserCreated(evento);

        verify(streamBridge).send(eq("user-created-topic"), eq(evento));
    }

    @Test
    void publishUserCreated_capturaEventoEnviado() {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent evento = new UserCreatedEvent(userId, "admin@empresa.cl", "admin");

        userEventProducer.publishUserCreated(evento);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq("user-created-topic"), captor.capture());

        UserCreatedEvent capturado = (UserCreatedEvent) captor.getValue();
        assertEquals("admin@empresa.cl", capturado.getEmail());
        assertEquals(userId, capturado.getUserId());
    }

    @Test
    void publishUserCreated_llamaStreamBridgeUnaVez() {
        userEventProducer.publishUserCreated(new UserCreatedEvent(UUID.randomUUID(), "x@test.cl", "cliente"));
        verify(streamBridge, times(1)).send(anyString(), any());
    }
}
