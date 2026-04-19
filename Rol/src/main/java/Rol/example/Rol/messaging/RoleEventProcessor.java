package Rol.example.Rol.messaging;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import Rol.example.Rol.dto.RoleAssignedEvent;
import Rol.example.Rol.dto.UserRegisteredEvent;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.service.RolService;


@Configuration
public class RoleEventProcessor {

    @Autowired
    private RolService rolService;

    
     
    @Bean
    public Function<UserRegisteredEvent, RoleAssignedEvent> processUserRegistration() {
        return event -> {
            System.out.println("⚡ KAFKA CONSUMER: Recibido evento de nuevo usuario en correo: " + event.getEmail());
            
            // Logica: Determinamos y asignamos su Rol
            RolModel assignedRole = rolService.assignRoleByEmail(event.getEmail());
            
            // Armamos nuestra respuesta al tópico
            RoleAssignedEvent responseEvent = new RoleAssignedEvent(
                event.getEmail(),
                assignedRole.getId(),
                assignedRole.getNombre()
            );
            
            System.out.println(" KAFKA PRODUCER: Notificando al sistema que se asginó rol [" + assignedRole.getNombre() + "]");
            return responseEvent;
        };
    }
}
