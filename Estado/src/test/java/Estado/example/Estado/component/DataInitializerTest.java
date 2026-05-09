package Estado.example.Estado.component;

import Estado.example.Estado.Component.DataInitializer;
import Estado.example.Estado.Repository.EstadoRepository;
import Estado.example.Estado.Repository.TipoDeEstadoRepository;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private TipoDeEstadoRepository tipoDeEstadoRepository;

    @Mock
    private EstadoRepository estadoRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_tiposYaExisten_noInsertaNada() throws Exception {
        when(tipoDeEstadoRepository.count()).thenReturn(4L);

        dataInitializer.run();

        verify(tipoDeEstadoRepository, never()).save(any());
        verify(estadoRepository, never()).saveAll(anyList());
    }

    @Test
    void run_tiposVacios_insertaTiposYEstados() throws Exception {
        TipoDeEstadoModel cuenta = new TipoDeEstadoModel(UUID.randomUUID(), "cuenta", "Tipo cuenta");
        TipoDeEstadoModel laboral = new TipoDeEstadoModel(UUID.randomUUID(), "laboral", "Tipo laboral");
        TipoDeEstadoModel producto = new TipoDeEstadoModel(UUID.randomUUID(), "producto", "Tipo producto");
        TipoDeEstadoModel envio = new TipoDeEstadoModel(UUID.randomUUID(), "envio", "Tipo envio");

        when(tipoDeEstadoRepository.count()).thenReturn(0L);
        when(tipoDeEstadoRepository.save(any(TipoDeEstadoModel.class)))
                .thenReturn(cuenta)
                .thenReturn(laboral)
                .thenReturn(producto)
                .thenReturn(envio);

        dataInitializer.run();

        verify(tipoDeEstadoRepository, times(4)).save(any(TipoDeEstadoModel.class));
        verify(estadoRepository).saveAll(anyList());
    }
}
