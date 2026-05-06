package Rol.example.Rol.service;

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

import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.TipoRepository;

@ExtendWith(MockitoExtension.class)
class PrivilegioServiceTest {

    @Mock
    private PrivilegioRepository privilegioRepository;

    @Mock
    private TipoRepository tipoRepository;

    @InjectMocks
    private PrivilegioService privilegioService;

    private TipoModel tipo;
    private PrivilegioModel privilegio;
    private UUID tipoId;
    private UUID privilegioId;

    @BeforeEach
    void setUp() {
        tipoId = UUID.randomUUID();
        privilegioId = UUID.randomUUID();
        tipo = new TipoModel(tipoId, "OPERACION");
        privilegio = new PrivilegioModel(privilegioId, "VER_INVENTARIO", "Ver stock de productos", tipo);
    }

    @Test
    void getAllPrivilegios_retornaLista() {
        when(privilegioRepository.findAll()).thenReturn(List.of(privilegio));

        List<PrivilegioModel> result = privilegioService.getAllPrivilegios();

        assertEquals(1, result.size());
        assertEquals("VER_INVENTARIO", result.get(0).getNombre());
    }

    @Test
    void getAllPrivilegios_listaVacia() {
        when(privilegioRepository.findAll()).thenReturn(List.of());

        assertTrue(privilegioService.getAllPrivilegios().isEmpty());
    }

    @Test
    void getPrivilegioById_encontrado() {
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));

        PrivilegioModel result = privilegioService.getPrivilegioById(privilegioId);

        assertEquals("VER_INVENTARIO", result.getNombre());
    }

    @Test
    void getPrivilegioById_noEncontrado_lanzaExcepcion() {
        when(privilegioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> privilegioService.getPrivilegioById(privilegioId));
    }

    @Test
    void getPrivilegiosByTipo_encontrado() {
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(privilegioRepository.findByTipoId(tipoId)).thenReturn(List.of(privilegio));

        List<PrivilegioModel> result = privilegioService.getPrivilegiosByTipo(tipoId);

        assertEquals(1, result.size());
    }

    @Test
    void getPrivilegiosByTipo_tipoNoExiste_lanzaExcepcion() {
        when(tipoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> privilegioService.getPrivilegiosByTipo(tipoId));
    }

    @Test
    void createPrivilegio_exitoso() {
        when(privilegioRepository.findByNombre("VER_INVENTARIO")).thenReturn(Optional.empty());
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(privilegioRepository.save(any(PrivilegioModel.class))).thenReturn(privilegio);

        PrivilegioModel result = privilegioService.createPrivilegio(privilegio);

        assertEquals("VER_INVENTARIO", result.getNombre());
        verify(privilegioRepository).save(privilegio);
    }

    @Test
    void createPrivilegio_nombreDuplicado_lanzaExcepcion() {
        when(privilegioRepository.findByNombre("VER_INVENTARIO")).thenReturn(Optional.of(privilegio));

        assertThrows(RuntimeException.class, () -> privilegioService.createPrivilegio(privilegio));
        verify(privilegioRepository, never()).save(any());
    }

    @Test
    void createPrivilegio_tipoNoExiste_lanzaExcepcion() {
        when(privilegioRepository.findByNombre("VER_INVENTARIO")).thenReturn(Optional.empty());
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> privilegioService.createPrivilegio(privilegio));
    }

    @Test
    void updatePrivilegio_exitoso() {
        PrivilegioModel actualizado = new PrivilegioModel(privilegioId, "EDITAR_INVENTARIO", "Editar stock", tipo);
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(privilegioRepository.save(any(PrivilegioModel.class))).thenReturn(actualizado);

        PrivilegioModel result = privilegioService.updatePrivilegio(privilegioId, actualizado);

        assertEquals("EDITAR_INVENTARIO", result.getNombre());
    }

    @Test
    void updatePrivilegio_noEncontrado_lanzaExcepcion() {
        when(privilegioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> privilegioService.updatePrivilegio(privilegioId, privilegio));
    }

    @Test
    void updatePrivilegio_sinTipo_actualizaSoloNombre() {
        PrivilegioModel sinTipo = new PrivilegioModel(null, "NUEVO", "desc", null);
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));
        when(privilegioRepository.save(any(PrivilegioModel.class))).thenReturn(privilegio);

        privilegioService.updatePrivilegio(privilegioId, sinTipo);

        verify(tipoRepository, never()).findById(any());
    }

    @Test
    void deletePrivilegio_exitoso() {
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));

        privilegioService.deletePrivilegio(privilegioId);

        verify(privilegioRepository).deleteById(privilegioId);
    }

    @Test
    void deletePrivilegio_noEncontrado_lanzaExcepcion() {
        when(privilegioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> privilegioService.deletePrivilegio(privilegioId));
        verify(privilegioRepository, never()).deleteById(any());
    }
}
