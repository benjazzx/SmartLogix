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

import Rol.example.Rol.model.TipoModel;
import Rol.example.Rol.repository.TipoRepository;

@ExtendWith(MockitoExtension.class)
class TipoServiceTest {

    @Mock
    private TipoRepository tipoRepository;

    @InjectMocks
    private TipoService tipoService;

    private TipoModel tipo;
    private UUID tipoId;

    @BeforeEach
    void setUp() {
        tipoId = UUID.randomUUID();
        tipo = new TipoModel(tipoId, "LECTURA");
    }

    @Test
    void getAllTipos_retornaLista() {
        when(tipoRepository.findAll()).thenReturn(List.of(tipo));

        List<TipoModel> result = tipoService.getAllTipos();

        assertEquals(1, result.size());
        assertEquals("LECTURA", result.get(0).getNombre());
        verify(tipoRepository).findAll();
    }

    @Test
    void getAllTipos_listaVacia() {
        when(tipoRepository.findAll()).thenReturn(List.of());

        List<TipoModel> result = tipoService.getAllTipos();

        assertTrue(result.isEmpty());
    }

    @Test
    void getTipoById_encontrado() {
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));

        TipoModel result = tipoService.getTipoById(tipoId);

        assertEquals("LECTURA", result.getNombre());
    }

    @Test
    void getTipoById_noEncontrado_lanzaExcepcion() {
        when(tipoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoService.getTipoById(tipoId));
    }

    @Test
    void getTipoByNombre_encontrado() {
        when(tipoRepository.findByNombre("LECTURA")).thenReturn(Optional.of(tipo));

        TipoModel result = tipoService.getTipoByNombre("LECTURA");

        assertEquals(tipoId, result.getId());
    }

    @Test
    void getTipoByNombre_noEncontrado_lanzaExcepcion() {
        when(tipoRepository.findByNombre("INEXISTENTE")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoService.getTipoByNombre("INEXISTENTE"));
    }

    @Test
    void createTipo_exitoso() {
        when(tipoRepository.findByNombre("LECTURA")).thenReturn(Optional.empty());
        when(tipoRepository.save(any(TipoModel.class))).thenReturn(tipo);

        TipoModel result = tipoService.createTipo(tipo);

        assertEquals("LECTURA", result.getNombre());
        verify(tipoRepository).save(tipo);
    }

    @Test
    void createTipo_nombreDuplicado_lanzaExcepcion() {
        when(tipoRepository.findByNombre("LECTURA")).thenReturn(Optional.of(tipo));

        assertThrows(RuntimeException.class, () -> tipoService.createTipo(tipo));
        verify(tipoRepository, never()).save(any());
    }

    @Test
    void updateTipo_exitoso() {
        TipoModel actualizado = new TipoModel(tipoId, "ESCRITURA");
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));
        when(tipoRepository.save(any(TipoModel.class))).thenReturn(actualizado);

        TipoModel result = tipoService.updateTipo(tipoId, actualizado);

        assertEquals("ESCRITURA", result.getNombre());
        verify(tipoRepository).save(any(TipoModel.class));
    }

    @Test
    void updateTipo_noEncontrado_lanzaExcepcion() {
        when(tipoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoService.updateTipo(tipoId, tipo));
    }

    @Test
    void deleteTipo_exitoso() {
        when(tipoRepository.findById(tipoId)).thenReturn(Optional.of(tipo));

        tipoService.deleteTipo(tipoId);

        verify(tipoRepository).deleteById(tipoId);
    }

    @Test
    void deleteTipo_noEncontrado_lanzaExcepcion() {
        when(tipoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tipoService.deleteTipo(tipoId));
        verify(tipoRepository, never()).deleteById(any());
    }
}
