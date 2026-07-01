package User.example.Users.messaging;

import User.example.Users.dto.EstadoAssignedEvent;
import User.example.Users.dto.RoleAssignedEvent;
import User.example.Users.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class UserEventConsumer {

    @Autowired
    private UserService userService;

    @Bean
    public Consumer<RoleAssignedEvent> roleAssignedConsumer() {
        return event -> {
            log.info("[Users CONSUMER] role-assigned-topic → correo={} rol={}", event.getEmail(), event.getRoleName());
            userService.actualizarRolPorCorreo(event.getEmail(), event.getRoleId(), event.getRoleName());
        };
    }

    @Bean
    public Consumer<EstadoAssignedEvent> estadoAssignedConsumer() {
        return event -> {
            log.info("[Users CONSUMER] estado-assigned-topic → correo={} estado={}", event.getEmail(), event.getEstadoNombre());
            userService.actualizarEstadoPorCorreo(event.getEmail(), event.getEstadoId(), event.getEstadoNombre());
        };
    }
}
