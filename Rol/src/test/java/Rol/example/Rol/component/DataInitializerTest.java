package Rol.example.Rol.component;

import Rol.example.Rol.model.PermisoModel;
import Rol.example.Rol.model.PrivilegioModel;
import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.PermisoRepository;
import Rol.example.Rol.repository.PrivilegioRepository;
import Rol.example.Rol.repository.RolRepository;
import Rol.example.Rol.repository.TipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private RolRepository rolRepository;
    @Mock private TipoRepository tipoRepository;
    @Mock private PrivilegioRepository privilegioRepository;
    @Mock private PermisoRepository permisoRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    private TipoModel tipo;
    private RolModel admin;
    private RolModel bodeguero;
    private RolModel transportista;
    private RolModel cliente;
    private PrivilegioModel privilegio;

    @BeforeEach
    void setUp() {
        tipo = new TipoModel(UUID.randomUUID(), "LECTURA");
        admin = new RolModel(UUID.randomUUID(), "admin", "Administrador total");
        bodeguero = new RolModel(UUID.randomUUID(), "bodeguero", "Gestión de bodega");
        transportista = new RolModel(UUID.randomUUID(), "transportista", "Coordinación de envíos");
        cliente = new RolModel(UUID.randomUUID(), "cliente", "Usuario final");
        privilegio = new PrivilegioModel(UUID.randomUUID(), "VER_INVENTARIO", "Ver stock", tipo);
    }

    @Test
    void run_reposYaInicializados_noInsertaNada() throws Exception {
        when(rolRepository.count()).thenReturn(4L);
        when(tipoRepository.count()).thenReturn(5L);
        when(privilegioRepository.count()).thenReturn(18L);
        when(permisoRepository.count()).thenReturn(30L);

        dataInitializer.run();

        verify(rolRepository, never()).saveAll(any());
        verify(tipoRepository, never()).saveAll(any());
        verify(privilegioRepository, never()).saveAll(any());
        verify(permisoRepository, never()).save(any());
    }

    @Test
    void run_soloRolesVacio_insertaRoles() throws Exception {
        when(rolRepository.count()).thenReturn(0L);
        when(tipoRepository.count()).thenReturn(5L);
        when(privilegioRepository.count()).thenReturn(18L);
        when(permisoRepository.count()).thenReturn(30L);

        dataInitializer.run();

        verify(rolRepository).saveAll(anyList());
        verify(tipoRepository, never()).saveAll(any());
    }

    @Test
    void run_soloTiposVacio_insertaTipos() throws Exception {
        when(rolRepository.count()).thenReturn(4L);
        when(tipoRepository.count()).thenReturn(0L);
        when(privilegioRepository.count()).thenReturn(18L);
        when(permisoRepository.count()).thenReturn(30L);

        dataInitializer.run();

        verify(tipoRepository).saveAll(anyList());
        verify(rolRepository, never()).saveAll(any());
    }

    @Test
    void run_todosReposVacios_insertaDatos() throws Exception {
        when(rolRepository.count()).thenReturn(0L);
        when(tipoRepository.count()).thenReturn(0L);
        when(privilegioRepository.count()).thenReturn(0L);
        when(permisoRepository.count()).thenReturn(0L);

        when(tipoRepository.findByNombre(anyString())).thenReturn(Optional.of(tipo));
        when(rolRepository.findByNombre("admin")).thenReturn(Optional.of(admin));
        when(rolRepository.findByNombre("bodeguero")).thenReturn(Optional.of(bodeguero));
        when(rolRepository.findByNombre("transportista")).thenReturn(Optional.of(transportista));
        when(rolRepository.findByNombre("cliente")).thenReturn(Optional.of(cliente));
        when(privilegioRepository.findAll()).thenReturn(List.of(privilegio));
        when(privilegioRepository.findByNombre(anyString())).thenReturn(Optional.of(privilegio));

        dataInitializer.run();

        verify(rolRepository).saveAll(anyList());
        verify(tipoRepository).saveAll(anyList());
        verify(privilegioRepository).saveAll(anyList());
        verify(permisoRepository, atLeastOnce()).save(any(PermisoModel.class));
    }

    @Test
    void run_soloPrivilegiosVacio_insertaPrivilegios() throws Exception {
        when(rolRepository.count()).thenReturn(4L);
        when(tipoRepository.count()).thenReturn(5L);
        when(privilegioRepository.count()).thenReturn(0L);
        when(permisoRepository.count()).thenReturn(30L);
        when(tipoRepository.findByNombre(anyString())).thenReturn(Optional.of(tipo));

        dataInitializer.run();

        verify(privilegioRepository).saveAll(anyList());
        verify(permisoRepository, never()).save(any());
    }

    @Test
    void run_soloPermisosVacio_insertaPermisos() throws Exception {
        when(rolRepository.count()).thenReturn(4L);
        when(tipoRepository.count()).thenReturn(5L);
        when(privilegioRepository.count()).thenReturn(18L);
        when(permisoRepository.count()).thenReturn(0L);
        when(rolRepository.findByNombre("admin")).thenReturn(Optional.of(admin));
        when(rolRepository.findByNombre("bodeguero")).thenReturn(Optional.of(bodeguero));
        when(rolRepository.findByNombre("transportista")).thenReturn(Optional.of(transportista));
        when(rolRepository.findByNombre("cliente")).thenReturn(Optional.of(cliente));
        when(privilegioRepository.findAll()).thenReturn(List.of(privilegio));
        when(privilegioRepository.findByNombre(anyString())).thenReturn(Optional.of(privilegio));

        dataInitializer.run();

        verify(permisoRepository, atLeastOnce()).save(any(PermisoModel.class));
        verify(rolRepository, never()).saveAll(any());
    }
}
