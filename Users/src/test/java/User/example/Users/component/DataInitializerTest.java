package User.example.Users.component;

import User.example.Users.dto.UserRequestDto;
import User.example.Users.model.ComunaModel;
import User.example.Users.model.DireccionModel;
import User.example.Users.model.RegionModel;
import User.example.Users.model.UserModel;
import User.example.Users.repository.*;
import User.example.Users.service.UserService;
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

    @Mock private RegionRepository regionRepository;
    @Mock private ComunaRepository comunaRepository;
    @Mock private DireccionRepository direccionRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_yaSembrado_noInsertaNada() throws Exception {
        when(regionRepository.count()).thenReturn(3L);
        when(comunaRepository.count()).thenReturn(4L);
        when(direccionRepository.count()).thenReturn(3L);
        when(userRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(4L);

        dataInitializer.run();

        verify(regionRepository, never()).saveAll(any());
        verify(comunaRepository, never()).saveAll(any());
        verify(direccionRepository, never()).saveAll(any());
        verify(userService, never()).createUser(any());
    }

    @Test
    void run_vacio_siembraEstructura() throws Exception {
        when(regionRepository.count()).thenReturn(0L);
        when(comunaRepository.count()).thenReturn(0L);
        when(direccionRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

        RegionModel rm = mock(RegionModel.class);
        when(regionRepository.findByNombre(anyString())).thenReturn(Optional.of(rm));

        ComunaModel cm = mock(ComunaModel.class);
        when(comunaRepository.findByNombre(anyString())).thenReturn(Optional.of(cm));

        DireccionModel d1 = mock(DireccionModel.class);
        DireccionModel d2 = mock(DireccionModel.class);
        DireccionModel d3 = mock(DireccionModel.class);
        when(d1.getId()).thenReturn(UUID.randomUUID());
        when(d2.getId()).thenReturn(UUID.randomUUID());
        when(d3.getId()).thenReturn(UUID.randomUUID());
        when(direccionRepository.findAll()).thenReturn(List.of(d1, d2, d3));

        dataInitializer.run();

        verify(regionRepository, atLeastOnce()).saveAll(any());
        verify(comunaRepository, atLeastOnce()).saveAll(any());
        verify(direccionRepository, atLeastOnce()).saveAll(any());
        verify(userService, times(4)).createUser(any(UserRequestDto.class));
    }

    @Test
    void run_corrigeRolIncorrecto_actualizaUsuario() throws Exception {
        when(regionRepository.count()).thenReturn(3L);
        when(comunaRepository.count()).thenReturn(4L);
        when(direccionRepository.count()).thenReturn(3L);
        when(userRepository.count()).thenReturn(3L);

        UserModel admin = mock(UserModel.class);
        when(admin.getRolNombre()).thenReturn("incorrecto");
        when(userRepository.findByCorreo("admin@smartlogix.cl")).thenReturn(Optional.of(admin));
        when(userRepository.findByCorreo("bodeguero@smartlogix.cl")).thenReturn(Optional.empty());
        when(userRepository.findByCorreo("transportista@smartlogix.cl")).thenReturn(Optional.empty());

        dataInitializer.run();

        verify(admin).setRolNombre("admin");
        verify(admin).setActivo(true);
        verify(userRepository).save(admin);
    }

    @Test
    void run_rolYaCorrecto_noActualiza() throws Exception {
        when(regionRepository.count()).thenReturn(3L);
        when(comunaRepository.count()).thenReturn(4L);
        when(direccionRepository.count()).thenReturn(3L);
        when(userRepository.count()).thenReturn(3L);

        UserModel admin = mock(UserModel.class);
        when(admin.getRolNombre()).thenReturn("admin");
        when(userRepository.findByCorreo("admin@smartlogix.cl")).thenReturn(Optional.of(admin));
        when(userRepository.findByCorreo("bodeguero@smartlogix.cl")).thenReturn(Optional.empty());
        when(userRepository.findByCorreo("transportista@smartlogix.cl")).thenReturn(Optional.empty());

        dataInitializer.run();

        verify(userRepository, never()).save(any());
    }
}
