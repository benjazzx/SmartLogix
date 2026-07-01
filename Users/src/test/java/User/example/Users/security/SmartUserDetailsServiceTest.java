package User.example.Users.security;

import User.example.Users.model.UserModel;
import User.example.Users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private SmartUserDetailsService service;

    @Test
    void loadUserByUsername_usuarioExiste_retornaUserDetails() {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo("admin@test.cl");
        user.setClave("$2a$10$hashedpassword");
        user.setRolNombre("admin");

        when(userRepository.findByCorreo("admin@test.cl")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.cl");

        assertNotNull(details);
        assertEquals("admin@test.cl", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin")));
    }

    @Test
    void loadUserByUsername_sinRol_usaRolPorDefecto() {
        UserModel user = new UserModel();
        user.setId(UUID.randomUUID());
        user.setCorreo("norole@test.cl");
        user.setClave("$2a$10$hashedpassword");
        user.setRolNombre(null);

        when(userRepository.findByCorreo("norole@test.cl")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("norole@test.cl");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_sin_rol")));
    }

    @Test
    void loadUserByUsername_usuarioNoExiste_lanzaExcepcion() {
        when(userRepository.findByCorreo("nope@test.cl")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nope@test.cl"));
    }
}
