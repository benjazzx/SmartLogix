package Rol.example.Rol.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Rol.example.Rol.model.RolModel;
import Rol.example.Rol.repository.RolRepository;

/**
 * Pruebas unitarias al 80%+ de cobertura usando Mockito.
 * Ignoramos la conexion a base de datos de verdad y simulamos "Mockeamos" las respuestas del repositorio.
 */
@ExtendWith(MockitoExtension.class)
public class RolServiceTest {

    @Mock
    private RolRepository rolRepository; // Simula comportamiento de la BD

    @InjectMocks
    private RolService rolService; // Usará la BD simulada (Mock) injectada

    private RolModel bodegueroRole;
    private RolModel adminRole;
    private RolModel transportistaRole;
    private RolModel clienteRole;

    @BeforeEach
    public void setUp() {
        // Se ejecuta antes de cada Test para inicializar datos falsos
        bodegueroRole = new RolModel(UUID.randomUUID(), "bodeguero", "Encargado de bodega");
        adminRole = new RolModel(UUID.randomUUID(), "admin", "Admin");
        transportistaRole = new RolModel(UUID.randomUUID(), "transportista", "Transport");
        clienteRole = new RolModel(UUID.randomUUID(), "cliente", "Cliente");
    }

    @Test
    public void testAssignBodegueroRoleByEmail() {
        // Cuando nuestro servicio pregunte por 'bodeguero' a la BD, devuelve este objeto falso sin conectarse a postgres
        when(rolRepository.findByNombre("bodeguero")).thenReturn(Optional.of(bodegueroRole));

        RolModel result = rolService.assignRoleByEmail("empleado@smartb.cl");

        assertNotNull(result);
        assertEquals("bodeguero", result.getNombre());
        verify(rolRepository, times(1)).findByNombre("bodeguero"); // Comprueba metodo ejecutado
    }

    @Test
    public void testAssignAdminRoleByEmail() {
        when(rolRepository.findByNombre("admin")).thenReturn(Optional.of(adminRole));

        RolModel result = rolService.assignRoleByEmail("super_jefe@smartadmin.cl");

        assertNotNull(result);
        assertEquals("admin", result.getNombre());
    }

    @Test
    public void testAssignTransportistaRoleByEmailSubDomain() {
        when(rolRepository.findByNombre("transportista")).thenReturn(Optional.of(transportistaRole));

        RolModel result = rolService.assignRoleByEmail("camion.juan@smartt.cl");

        assertNotNull(result);
        assertEquals("transportista", result.getNombre());
    }

    @Test
    public void testAssignClienteRoleForGenericEmail() {
        when(rolRepository.findByNombre("cliente")).thenReturn(Optional.of(clienteRole));

        RolModel result = rolService.assignRoleByEmail("usuario123@gmail.com");

        assertNotNull(result);
        assertEquals("cliente", result.getNombre());
    }

    @Test
    public void testFallbackToClienteIfRoleDatabaseMissing() {
        // Si no existe 'bodeguero' devuelto por BD, nuestro sistema debe regresar como fallback al 'cliente'
        when(rolRepository.findByNombre("bodeguero")).thenReturn(Optional.empty()); // no lo encuentra
        when(rolRepository.findByNombre("cliente")).thenReturn(Optional.of(clienteRole)); // agarra cliente

        RolModel result = rolService.assignRoleByEmail("error@smartb.cl");

        assertNotNull(result);
        assertEquals("cliente", result.getNombre());
    }
}
