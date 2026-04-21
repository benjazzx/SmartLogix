package Estado.example.Estado.Messaging;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import Estado.example.Estado.Dto.EstadoAssignedEvent;
import Estado.example.Estado.Dto.RoleAssignedEvent;
import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Service.EstadoService;

@Configuration
public class EstadoEventProcessor {

    @Autowired
    private EstadoService estadoService;

    @Bean
    public Function<RoleAssignedEvent, EstadoAssignedEvent> processRoleAssigned() {
        return event -> {
            System.out.println(" KAFKA CONSUMER: Recibido evento de rol asignado para: " + event.getEmail() + " con rol: " + event.getRoleName());

            Estado assigned = estadoService.assignEstadoByRol(event.getRoleName());

            EstadoAssignedEvent response = new EstadoAssignedEvent(
                event.getEmail(),
                assigned.getId(),
                assigned.getNombre(),
                assigned.getTipoDeEstado().getNombre()
            );

            System.out.println(" KAFKA PRODUCER: Notificando que se asignó estado [" + assigned.getNombre() + "] de tipo [" + assigned.getTipoDeEstado().getNombre() + "]");
            return response;
        };
    }
}
