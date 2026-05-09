package Inventario.example.Inventario.config;

import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
import Inventario.example.Inventario.repository.PasilloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private BodegaRepository bodegaRepository;
    @Mock
    private PasilloRepository pasilloRepository;
    @Mock
    private EstanteRepository estanteRepository;
    @Mock
    private EstPasiRepository estPasiRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_inventarioYaInicializado_noInsertaNada() {
        when(bodegaRepository.count()).thenReturn(2L);

        dataInitializer.run();

        verify(bodegaRepository, never()).save(any());
        verify(pasilloRepository, never()).save(any());
        verify(estanteRepository, never()).save(any());
        verify(estPasiRepository, never()).save(any());
    }

    @Test
    void run_inventarioVacio_insertaDatos() {
        when(bodegaRepository.count()).thenReturn(0L);
        when(bodegaRepository.save(any(BodegaModel.class))).thenReturn(mock(BodegaModel.class));
        when(pasilloRepository.save(any(PasilloModel.class))).thenReturn(mock(PasilloModel.class));
        when(estanteRepository.save(any(EstanteModel.class))).thenReturn(mock(EstanteModel.class));
        when(estPasiRepository.save(any(EstPasiModel.class))).thenReturn(mock(EstPasiModel.class));

        dataInitializer.run();

        verify(bodegaRepository, atLeastOnce()).save(any(BodegaModel.class));
        verify(pasilloRepository, atLeastOnce()).save(any(PasilloModel.class));
        verify(estanteRepository, atLeastOnce()).save(any(EstanteModel.class));
        verify(estPasiRepository, atLeastOnce()).save(any(EstPasiModel.class));
    }
}
