package Rol.example.Rol.messaging;

import Rol.example.Rol.dto.UserRegisteredEvent;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class UserEventConsumer {

    @Autowired private RolService rolService;

    @Bean
    public Consumer<UserRegisteredEvent> userRegisteredConsumer() {
        return event -> {
            log.info("[CONSUMER] user-created-topic → email={}", event.getEmail());
            RolModel rol = rolService.assignRoleByEmail(event.getEmail());
            log.info("[PRODUCER] role-assigned-topic → rol={} para email={}", rol != null ? rol.getNombre() : "null", event.getEmail());
        };
    }
}
