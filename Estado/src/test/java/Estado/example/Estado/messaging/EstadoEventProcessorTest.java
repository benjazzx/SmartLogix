package Estado.example.Estado.messaging;

import Estado.example.Estado.Dto.EstadoAssignedEvent;
import Estado.example.Estado.Dto.RoleAssignedEvent;
import Estado.example.Estado.Messaging.EstadoEventProcessor;
import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Service.EstadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstadoEventProcessorTest {

    @Mock
    private EstadoService estadoService;

    @InjectMocks
    private EstadoEventProcessor estadoEventProcessor;

    @Test
    void processRoleAssigned_asignaEstadoYRetornaEvento() {
        TipoDeEstadoModel tipo = new TipoDeEstadoModel(UUID.randomUUID(), "cuenta", "Tipo cuenta");
        Estado estadoAsignado = new Estado(UUID.randomUUID(), "activo", "Usuario activo", tipo);
        RoleAssignedEvent evento = new RoleAssignedEvent("usuario@test.cl", UUID.randomUUID(), "admin");

        when(estadoService.assignEstadoByRol("admin")).thenReturn(estadoAsignado);

        Function<RoleAssignedEvent, EstadoAssignedEvent> fn = estadoEventProcessor.processRoleAssigned();
        EstadoAssignedEvent resultado = fn.apply(evento);

        assertEquals("usuario@test.cl", resultado.getEmail());
        assertEquals(estadoAsignado.getId(), resultado.getEstadoId());
        assertEquals("activo", resultado.getEstadoNombre());
        assertEquals("cuenta", resultado.getTipoEstadoNombre());
        verify(estadoService).assignEstadoByRol("admin");
    }

    @Test
    void processRoleAssigned_distintoRol_procesaCorrectamente() {
        TipoDeEstadoModel tipo = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Tipo laboral");
        Estado estadoAsignado = new Estado(UUID.randomUUID(), "disponible", "Empleado disponible", tipo);
        RoleAssignedEvent evento = new RoleAssignedEvent("emp@empresa.cl", UUID.randomUUID(), "transportista");

        when(estadoService.assignEstadoByRol("transportista")).thenReturn(estadoAsignado);

        Function<RoleAssignedEvent, EstadoAssignedEvent> fn = estadoEventProcessor.processRoleAssigned();
        EstadoAssignedEvent resultado = fn.apply(evento);

        assertEquals("emp@empresa.cl", resultado.getEmail());
        assertEquals("disponible", resultado.getEstadoNombre());
        assertEquals("laboral", resultado.getTipoEstadoNombre());
    }
}
