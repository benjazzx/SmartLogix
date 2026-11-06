package Configuracion.example.Configuracion.service;

import Configuracion.example.Configuracion.model.PreferenciaModel;
import Configuracion.example.Configuracion.repository.PreferenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceOnboardingTest {

    @Mock private PreferenciaRepository repository;
    @InjectMocks private ConfiguracionService service;

    @Test
    void crearPreferenciaInicial_siNoExiste_laCrea() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndClave(userId, "onboarding")).thenReturn(Optional.empty());

        boolean creada = service.crearPreferenciaInicial(userId, "onboarding", "pendiente");

        assertTrue(creada);
        ArgumentCaptor<PreferenciaModel> captor = ArgumentCaptor.forClass(PreferenciaModel.class);
        verify(repository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals("onboarding", captor.getValue().getClave());
        assertEquals("pendiente", captor.getValue().getValor());
    }

    @Test
    void crearPreferenciaInicial_siYaExiste_noLaPisa() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndClave(userId, "onboarding"))
                .thenReturn(Optional.of(new PreferenciaModel(userId, "onboarding", "completado")));

        boolean creada = service.crearPreferenciaInicial(userId, "onboarding", "pendiente");

        assertFalse(creada);
        verify(repository, never()).save(any());
    }
}
