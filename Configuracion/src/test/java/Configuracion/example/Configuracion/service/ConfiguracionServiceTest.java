package Configuracion.example.Configuracion.service;

import Configuracion.example.Configuracion.model.PreferenciaModel;
import Configuracion.example.Configuracion.repository.PreferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S100")
@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private PreferenciaRepository repository;

    @InjectMocks
    private ConfiguracionService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getPreferencias_conPreferenciasExistentes_retornaMapaCorrecto() {
        PreferenciaModel pref = new PreferenciaModel(userId, "tema", "esmeralda");
        when(repository.findAllByUserId(userId)).thenReturn(List.of(pref));

        Map<String, String> result = service.getPreferencias(userId);

        assertEquals(1, result.size());
        assertEquals("esmeralda", result.get("tema"));
    }

    @Test
    void getPreferencias_sinPreferencias_retornaMapaVacio() {
        when(repository.findAllByUserId(userId)).thenReturn(List.of());

        Map<String, String> result = service.getPreferencias(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void setPreferencia_nuevaClave_creaRegistro() {
        when(repository.findByUserIdAndClave(userId, "tema")).thenReturn(Optional.empty());

        service.setPreferencia(userId, "tema", "oceano");

        verify(repository, times(1)).save(any(PreferenciaModel.class));
    }

    @Test
    void setPreferencia_claveExistente_actualizaValor() {
        PreferenciaModel existente = new PreferenciaModel(userId, "tema", "esmeralda");
        when(repository.findByUserIdAndClave(userId, "tema")).thenReturn(Optional.of(existente));

        service.setPreferencia(userId, "tema", "marino");

        assertEquals("marino", existente.getValor());
        verify(repository, times(1)).save(existente);
    }

    @Test
    void getPreferencia_claveExistente_retornaValor() {
        PreferenciaModel pref = new PreferenciaModel(userId, "tema", "violeta");
        when(repository.findByUserIdAndClave(userId, "tema")).thenReturn(Optional.of(pref));

        String result = service.getPreferencia(userId, "tema");

        assertEquals("violeta", result);
    }

    @Test
    void getPreferencia_claveInexistente_retornaNull() {
        when(repository.findByUserIdAndClave(userId, "tema")).thenReturn(Optional.empty());

        String result = service.getPreferencia(userId, "tema");

        assertNull(result);
    }
}
