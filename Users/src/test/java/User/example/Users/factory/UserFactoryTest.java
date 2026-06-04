package User.example.Users.factory;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.UserModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    private UserRequestDto buildDto(String rolNombre) {
        UserRequestDto dto = new UserRequestDto();
        dto.setNombre("Juan");
        dto.setApellido("Perez");
        dto.setRut("12345678-9");
        dto.setCorreo("juan@test.cl");
        dto.setClave("Pass123!");
        dto.setRolNombre(rolNombre);
        dto.setRolId(UUID.randomUUID());
        return dto;
    }

    @Test
    void obtenerFactory_cliente_retornaClienteFactory() {
        UserFactory factory = UserFactory.obtenerFactory("cliente");
        assertInstanceOf(ClienteFactory.class, factory);
    }

    @Test
    void obtenerFactory_clienteMayusculas_retornaClienteFactory() {
        UserFactory factory = UserFactory.obtenerFactory("CLIENTE");
        assertInstanceOf(ClienteFactory.class, factory);
    }

    @Test
    void obtenerFactory_nulo_retornaEmpleadoFactory() {
        UserFactory factory = UserFactory.obtenerFactory(null);
        assertInstanceOf(EmpleadoFactory.class, factory);
    }

    @Test
    void obtenerFactory_otroRol_retornaEmpleadoFactory() {
        UserFactory factory = UserFactory.obtenerFactory("admin");
        assertInstanceOf(EmpleadoFactory.class, factory);
    }

    @Test
    void clienteFactory_sinCargo_asignaCargoDefecto() {
        UserRequestDto dto = buildDto("cliente");
        dto.setCargo(null);

        UserModel user = new ClienteFactory().crearUsuario(dto);

        assertEquals("Cliente", user.getCargo());
        assertEquals("juan@test.cl", user.getCorreo());
        assertTrue(user.getActivo());
    }

    @Test
    void clienteFactory_conCargo_usaCargo() {
        UserRequestDto dto = buildDto("cliente");
        dto.setCargo("VIP");

        UserModel user = new ClienteFactory().crearUsuario(dto);

        assertEquals("VIP", user.getCargo());
    }

    @Test
    void empleadoFactory_crearUsuario_mapeoCompleto() {
        UserRequestDto dto = buildDto("bodeguero");
        dto.setCargo("Bodeguero Senior");

        UserModel user = new EmpleadoFactory().crearUsuario(dto);

        assertEquals("Juan", user.getNombre());
        assertEquals("Perez", user.getApellido());
        assertEquals("juan@test.cl", user.getCorreo());
        assertEquals("Bodeguero Senior", user.getCargo());
        assertEquals("bodeguero", user.getRolNombre());
        assertTrue(user.getActivo());
    }

    @Test
    void empleadoFactory_cargoNulo_quedaNulo() {
        UserRequestDto dto = buildDto("admin");
        dto.setCargo(null);

        UserModel user = new EmpleadoFactory().crearUsuario(dto);

        assertNull(user.getCargo());
    }
}
