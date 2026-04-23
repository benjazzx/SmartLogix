package User.example.Users.messaging;

import User.example.Users.dto.UserCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

// Publicador de eventos Kafka usando StreamBridge (no requiere declarar un @Bean Function)
// Patrón orientado a eventos: Users notifica al resto del sistema cuando ocurre un cambio de estado
@Component
public class UserEventProducer {

    @Autowired
    private StreamBridge streamBridge;

    // Publica en "user-created-topic" cuando se registra un nuevo usuario
    // Estado puede consumir este evento para asignar el estado inicial (activo) automáticamente
    public void publishUserCreated(UserCreatedEvent event) {
        streamBridge.send("user-created-topic", event);
        System.out.println("[Users PRODUCER] Evento publicado en user-created-topic para: " + event.getEmail());
    }
}
