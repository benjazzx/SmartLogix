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

import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Repository.TipoDeEstadoRepository;
import Estado.example.Estado.Service.TipoDeEstadoService;

@ExtendWith(MockitoExtension.class)
public class TipoDeEstadoServiceTest {

    @Mock
    private TipoDeEstadoRepository tipoDeEstadoRepository;

    @InjectMocks
    private TipoDeEstadoService tipoDeEstadoService;

    private TipoDeEstadoModel tipoCuenta;
    private TipoDeEstadoModel tipoLaboral;
    private UUID tipoId;

    @BeforeEach
    void setUp() {
        tipoId = UUID.randomUUID();
        tipoCuenta  = new TipoDeEstadoModel(tipoId, "cuenta", "Estados de cuenta de usuario");
        tipoLaboral = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Estados laborales");
    }

    @Test
    void testGetAll_retornaLista() {
        when(tipoDeEstadoRepository.findAll()).thenReturn(List.of(tipoCuenta, tipoLaboral));

        List<TipoDeEstadoModel> result = tipoDeEstadoService.getAll();

        assertEquals(2, result.size());
        verify(tipoDeEstadoRepository, times(1)).findAll();
    }

    @Test
    void testGetAll_listaVacia() {
        when(tipoDeEstadoRepository.findAll()).thenReturn(List.of());

        assertTrue(tipoDeEstadoService.getAll().isEmpty());
    }

    @Test
    void testGetById_encontrado() {
        when(tipoDeEstadoRepository.findById(tipoId)).thenReturn(Optional.of(tipoCuenta));

        TipoDeEstadoModel result = tipoDeEstadoService.getById(tipoId);

        assertNotNull(result);
        assertEquals("cuenta", result.getNombre());
    }

    @Test
    void testGetById_noEncontrado_lanzaExcepcion() {
        when(tipoDeEstadoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoDeEstadoService.getById(UUID.randomUUID()));
    }

    @Test
    void testGetByNombre_encontrado() {
        when(tipoDeEstadoRepository.findByNombre("laboral")).thenReturn(Optional.of(tipoLaboral));

        TipoDeEstadoModel result = tipoDeEstadoService.getByNombre("laboral");

        assertNotNull(result);
        assertEquals("laboral", result.getNombre());
    }

    @Test
    void testGetByNombre_noExiste_retornaNull() {
        when(tipoDeEstadoRepository.findByNombre("inexistente")).thenReturn(Optional.empty());

        assertNull(tipoDeEstadoService.getByNombre("inexistente"));
    }

    @Test
    void testCreate_guardaYRetorna() {
        when(tipoDeEstadoRepository.save(tipoCuenta)).thenReturn(tipoCuenta);

        TipoDeEstadoModel result = tipoDeEstadoService.create(tipoCuenta);

        assertNotNull(result);
        assertEquals("cuenta", result.getNombre());
        verify(tipoDeEstadoRepository, times(1)).save(tipoCuenta);
    }

    @Test
    void testUpdate_encontrado_actualizaCampos() {
        TipoDeEstadoModel actualizado = new TipoDeEstadoModel(null, "producto", "Estados de producto");
        when(tipoDeEstadoRepository.findById(tipoId)).thenReturn(Optional.of(tipoCuenta));
        when(tipoDeEstadoRepository.save(any(TipoDeEstadoModel.class))).thenReturn(tipoCuenta);

        TipoDeEstadoModel result = tipoDeEstadoService.update(tipoId, actualizado);

        assertNotNull(result);
        verify(tipoDeEstadoRepository, times(1)).save(any(TipoDeEstadoModel.class));
    }

    @Test
    void testUpdate_noEncontrado_lanzaExcepcion() {
        when(tipoDeEstadoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoDeEstadoService.update(UUID.randomUUID(), tipoCuenta));
    }

    @Test
    void testDelete_llamaRepositorio() {
        doNothing().when(tipoDeEstadoRepository).deleteById(tipoId);

        tipoDeEstadoService.delete(tipoId);

        verify(tipoDeEstadoRepository, times(1)).deleteById(tipoId);
    }
}
