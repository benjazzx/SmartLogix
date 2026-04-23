package Rol.example.Rol.messaging;

import Rol.example.Rol.dto.UserRegisteredEvent;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

// Consumer Kafka: escucha "user-created-topic" publicado por el microservicio Users.
// Cuando un usuario se registra, Rol lo detecta y asigna automáticamente el rol
// basándose en el dominio del email — luego publica role-assigned-topic para
// que Estado y Users actualicen sus registros.
@Configuration
public class UserEventConsumer {

    @Autowired private RolService rolService;

    @Bean
    public Consumer<UserRegisteredEvent> userRegisteredConsumer() {
        return event -> {
            System.out.println("[Rol CONSUMER] user-created-topic → email: " + event.getEmail());
            // Si el usuario ya tiene rolNombre asignado, verificar que existe; si no, auto-asignar
            RolModel rol = rolService.assignRoleByEmail(event.getEmail());
            System.out.println("[Rol PRODUCER] role-assigned-topic → rol: "
                + (rol != null ? rol.getNombre() : "null") + " para " + event.getEmail());
        };
    }
}
