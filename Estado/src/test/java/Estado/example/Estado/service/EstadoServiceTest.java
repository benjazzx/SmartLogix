package Estado.example.Estado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Repository.EstadoRepository;
import Estado.example.Estado.Service.EstadoService;

@ExtendWith(MockitoExtension.class)
public class EstadoServiceTest {

    @Mock
    private EstadoRepository estadoRepository;

    @InjectMocks
    private EstadoService estadoService;

    private TipoDeEstadoModel tipoCuenta;
    private TipoDeEstadoModel tipoLaboral;
    private Estado estadoActivo;
    private Estado estadoDisponible;
    private Estado estadoPendiente;
    private UUID estadoId;

    @BeforeEach
    void setUp() {
        tipoCuenta  = new TipoDeEstadoModel(UUID.randomUUID(), "cuenta", "Estados de cuenta");
        tipoLaboral = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Estados laborales");
        estadoId = UUID.randomUUID();
        estadoActivo     = new Estado(estadoId,            "activo",                 "Usuario activo",              tipoCuenta);
        estadoDisponible = new Estado(UUID.randomUUID(),   "disponible",             "Empleado disponible",         tipoLaboral);
        estadoPendiente  = new Estado(UUID.randomUUID(),   "pendiente_verificacion", "Cuenta pendiente",            tipoCuenta);
    }

    @Test
    void testGetAll_retornaLista() {
        when(estadoRepository.findAll()).thenReturn(List.of(estadoActivo, estadoDisponible));

        List<Estado> result = estadoService.getAll();

        assertEquals(2, result.size());
        verify(estadoRepository, times(1)).findAll();
    }

    @Test
    void testGetById_encontrado() {
        when(estadoRepository.findById(estadoId)).thenReturn(Optional.of(estadoActivo));

        Estado result = estadoService.getById(estadoId);

        assertNotNull(result);
        assertEquals("activo", result.getNombre());
    }

    @Test
    void testGetById_noEncontrado_lanzaExcepcion() {
        when(estadoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> estadoService.getById(UUID.randomUUID()));
    }

    @Test
    void testGetByNombre_encontrado() {
        when(estadoRepository.findByNombre("activo")).thenReturn(Optional.of(estadoActivo));

        Estado result = estadoService.getByNombre("activo");

        assertNotNull(result);
        assertEquals("activo", result.getNombre());
    }

    @Test
    void testGetByNombre_noExiste_retornaNull() {
        when(estadoRepository.findByNombre("inexistente")).thenReturn(Optional.empty());

        assertNull(estadoService.getByNombre("inexistente"));
    }

    @Test
    void testGetByTipoNombre_retornaLista() {
        when(estadoRepository.findByTipoDeEstadoNombre("cuenta")).thenReturn(List.of(estadoActivo, estadoPendiente));

        List<Estado> result = estadoService.getByTipoNombre("cuenta");

        assertEquals(2, result.size());
    }

    @Test
    void testCreate_guardaYRetorna() {
        when(estadoRepository.save(estadoActivo)).thenReturn(estadoActivo);

        Estado result = estadoService.create(estadoActivo);

        assertNotNull(result);
        assertEquals("activo", result.getNombre());
        verify(estadoRepository, times(1)).save(estadoActivo);
    }

    @Test
    void testUpdate_encontrado_actualizaCampos() {
        Estado actualizado = new Estado(null, "inactivo", "Usuario inactivo", tipoCuenta);
        when(estadoRepository.findById(estadoId)).thenReturn(Optional.of(estadoActivo));
        when(estadoRepository.save(any(Estado.class))).thenReturn(estadoActivo);

        Estado result = estadoService.update(estadoId, actualizado);

        assertNotNull(result);
        verify(estadoRepository, times(1)).save(any(Estado.class));
    }

    @Test
    void testUpdate_noEncontrado_lanzaExcepcion() {
        when(estadoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> estadoService.update(UUID.randomUUID(), estadoActivo));
    }

    @Test
    void testDelete_llamaRepositorio() {
        doNothing().when(estadoRepository).deleteById(estadoId);

        estadoService.delete(estadoId);

        verify(estadoRepository, times(1)).deleteById(estadoId);
    }

    @Test
    void testAssignEstadoByRol_bodeguero_retornaDisponible() {
        when(estadoRepository.findByNombre("disponible")).thenReturn(Optional.of(estadoDisponible));

        Estado result = estadoService.assignEstadoByRol("bodeguero");

        assertNotNull(result);
        assertEquals("disponible", result.getNombre());
        assertEquals("laboral", result.getTipoDeEstado().getNombre());
    }

    @Test
    void testAssignEstadoByRol_transportista_retornaDisponible() {
        when(estadoRepository.findByNombre("disponible")).thenReturn(Optional.of(estadoDisponible));

        Estado result = estadoService.assignEstadoByRol("transportista");

        assertEquals("disponible", result.getNombre());
    }

    @Test
    void testAssignEstadoByRol_admin_retornaActivo() {
        when(estadoRepository.findByNombre("activo")).thenReturn(Optional.of(estadoActivo));

        Estado result = estadoService.assignEstadoByRol("admin");

        assertEquals("activo", result.getNombre());
        assertEquals("cuenta", result.getTipoDeEstado().getNombre());
    }

    @Test
    void testAssignEstadoByRol_cliente_retornaPendiente() {
        when(estadoRepository.findByNombre("pendiente_verificacion")).thenReturn(Optional.of(estadoPendiente));

        Estado result = estadoService.assignEstadoByRol("cliente");

        assertEquals("pendiente_verificacion", result.getNombre());
    }

    @Test
    void testAssignEstadoByRol_rolNull_retornaPendiente() {
        when(estadoRepository.findByNombre("pendiente_verificacion")).thenReturn(Optional.of(estadoPendiente));

        Estado result = estadoService.assignEstadoByRol(null);

        assertEquals("pendiente_verificacion", result.getNombre());
    }

    @Test
    void testAssignEstadoByRol_estadoNoEnBD_fallbackAPendiente() {
        when(estadoRepository.findByNombre("disponible")).thenReturn(Optional.empty());
        when(estadoRepository.findByNombre("pendiente_verificacion")).thenReturn(Optional.of(estadoPendiente));

        Estado result = estadoService.assignEstadoByRol("bodeguero");

        assertEquals("pendiente_verificacion", result.getNombre());
    }
}
