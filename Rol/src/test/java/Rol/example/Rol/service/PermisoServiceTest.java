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

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.PermisoRepository;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.RolRepository;

@ExtendWith(MockitoExtension.class)
class PermisoServiceTest {

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PrivilegioRepository privilegioRepository;

    @InjectMocks
    private PermisoService permisoService;

    private RolModel rol;
    private PrivilegioModel privilegio;
    private PermisoModel permiso;
    private UUID rolId;
    private UUID privilegioId;
    private UUID permisoId;

    @BeforeEach
    void setUp() {
        rolId = UUID.randomUUID();
        privilegioId = UUID.randomUUID();
        permisoId = UUID.randomUUID();
        TipoModel tipo = new TipoModel(UUID.randomUUID(), "OPERACION");
        rol = new RolModel(rolId, "bodeguero", "Encargado de bodega");
        privilegio = new PrivilegioModel(privilegioId, "VER_INVENTARIO", "Ver stock", tipo);
        permiso = new PermisoModel(permisoId, rol, privilegio);
    }

    @Test
    void getAllPermisos_retornaLista() {
        when(permisoRepository.findAll()).thenReturn(List.of(permiso));

        List<PermisoModel> result = permisoService.getAllPermisos();

        assertEquals(1, result.size());
        verify(permisoRepository).findAll();
    }

    @Test
    void getAllPermisos_listaVacia() {
        when(permisoRepository.findAll()).thenReturn(List.of());

        assertTrue(permisoService.getAllPermisos().isEmpty());
    }

    @Test
    void getPermisosByRol_rolExistente() {
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(permisoRepository.findByRolId(rolId)).thenReturn(List.of(permiso));

        List<PermisoModel> result = permisoService.getPermisosByRol(rolId);

        assertEquals(1, result.size());
    }

    @Test
    void getPermisosByRol_rolNoExiste_lanzaExcepcion() {
        when(rolRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> permisoService.getPermisosByRol(rolId));
    }

    @Test
    void tienePermiso_cuandoExiste_retornaTrue() {
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(true);

        assertTrue(permisoService.tienePermiso(rolId, privilegioId));
    }

    @Test
    void tienePermiso_cuandoNoExiste_retornaFalse() {
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(false);

        assertFalse(permisoService.tienePermiso(rolId, privilegioId));
    }

    @Test
    void asignarPermiso_exitoso() {
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(false);
        when(permisoRepository.save(any(PermisoModel.class))).thenReturn(permiso);

        PermisoModel result = permisoService.asignarPermiso(rolId, privilegioId);

        assertNotNull(result);
        verify(permisoRepository).save(any(PermisoModel.class));
    }

    @Test
    void asignarPermiso_rolNoExiste_lanzaExcepcion() {
        when(rolRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> permisoService.asignarPermiso(rolId, privilegioId));
    }

    @Test
    void asignarPermiso_privilegioNoExiste_lanzaExcepcion() {
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(privilegioRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> permisoService.asignarPermiso(rolId, privilegioId));
    }

    @Test
    void asignarPermiso_yaAsignado_lanzaExcepcion() {
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rol));
        when(privilegioRepository.findById(privilegioId)).thenReturn(Optional.of(privilegio));
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> permisoService.asignarPermiso(rolId, privilegioId));
        verify(permisoRepository, never()).save(any());
    }

    @Test
    void revocarPermiso_exitoso() {
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(true);

        permisoService.revocarPermiso(rolId, privilegioId);

        verify(permisoRepository).deleteByRolIdAndPrivilegioId(rolId, privilegioId);
    }

    @Test
    void revocarPermiso_noAsignado_lanzaExcepcion() {
        when(permisoRepository.existsByRolIdAndPrivilegioId(rolId, privilegioId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> permisoService.revocarPermiso(rolId, privilegioId));
    }

    @Test
    void deletePermiso_exitoso() {
        when(permisoRepository.findById(permisoId)).thenReturn(Optional.of(permiso));

        permisoService.deletePermiso(permisoId);

        verify(permisoRepository).deleteById(permisoId);
    }

    @Test
    void deletePermiso_noEncontrado_lanzaExcepcion() {
        when(permisoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> permisoService.deletePermiso(permisoId));
        verify(permisoRepository, never()).deleteById(any());
    }
}
