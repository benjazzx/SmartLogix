package User.example.Users.messaging;

import User.example.Users.dto.EstadoAssignedEvent;
import User.example.Users.dto.RoleAssignedEvent;
import User.example.Users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

// Consumidores de eventos Kafka usando Spring Cloud Stream
// Patrón orientado a eventos: Users reacciona a cambios publicados por Rol y Estado
// sin necesitar llamadas HTTP directas entre microservicios (desacoplamiento total)
@Configuration
public class UserEventConsumer {

    @Autowired
    private UserService userService;

    // Consume "role-assigned-topic" publicado por el microservicio Rol
    // Cuando un rol es asignado a un usuario, actualiza rolId y rolNombre en la BD local
    @Bean
    public Consumer<RoleAssignedEvent> roleAssignedConsumer() {
        return event -> {
            System.out.println("[Users CONSUMER] role-assigned-topic → correo: "
                + event.getEmail() + ", rol: " + event.getRoleName());
            userService.actualizarRolPorCorreo(
                event.getEmail(),
                event.getRoleId(),
                event.getRoleName()
            );
        };
    }

    // Consume "estado-assigned-topic" publicado por el microservicio Estado
    // Cuando se asigna un estado al usuario, actualiza estadoId, estadoNombre y activo
    @Bean
    public Consumer<EstadoAssignedEvent> estadoAssignedConsumer() {
        return event -> {
            System.out.println("[Users CONSUMER] estado-assigned-topic → correo: "
                + event.getEmail() + ", estado: " + event.getEstadoNombre());
            userService.actualizarEstadoPorCorreo(
                event.getEmail(),
                event.getEstadoId(),
                event.getEstadoNombre()
            );
        };
    }
}
