package Estado.example.Estado.Messaging;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import Estado.example.Estado.Dto.EstadoAssignedEvent;
import Estado.example.Estado.Dto.RoleAssignedEvent;
import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Service.EstadoService;

@Slf4j
@Configuration
public class EstadoEventProcessor {

    @Autowired
    private EstadoService estadoService;

    @Bean
    public Function<RoleAssignedEvent, EstadoAssignedEvent> processRoleAssigned() {
        return event -> {
            log.info("[CONSUMER] role-assigned-topic → email={} rol={}", event.getEmail(), event.getRoleName());

            Estado assigned = estadoService.assignEstadoByRol(event.getRoleName());

            EstadoAssignedEvent response = new EstadoAssignedEvent(
                event.getEmail(),
                assigned.getId(),
                assigned.getNombre(),
                assigned.getTipoDeEstado().getNombre()
            );

            log.info("[PRODUCER] estado-assigned-topic → estado={} tipo={}", assigned.getNombre(), assigned.getTipoDeEstado().getNombre());
            return response;
        };
    }
}
