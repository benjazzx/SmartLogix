package User.example.Users.messaging;

import User.example.Users.dto.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventProducer {

    @Autowired
    private StreamBridge streamBridge;

    public void publishUserCreated(UserCreatedEvent event) {
        streamBridge.send("user-created-topic", event);
        log.info("[Users PRODUCER] user-created-topic → email={}", event.getEmail());
    }
}
