package Inventario.example.Inventario.service;

import Inventario.example.Inventario.dto.OrdenBodegaAsignadaRabbitEvent;
import Inventario.example.Inventario.dto.ProductoUbicacionAsignadaRabbitEvent;
import Inventario.example.Inventario.exception.SinCapacidadException;
import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.EstPasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticaServiceTest {

    @Mock private EstPasiRepository estPasiRepository;
    @Mock private BodegaRepository bodegaRepository;

    private LogisticaService service;

    @BeforeEach
    void setUp() {
        service = new LogisticaService(estPasiRepository, bodegaRepository);
    }

    private BodegaModel bodega(long id, String nombre) {
        return BodegaModel.builder().idBodega(id).nombre(nombre).build();
    }

    private EstPasiModel ubicacion(long idEstante, long idPasillo, BodegaModel bodega, Double ocupacion) {
        return EstPasiModel.builder()
                .estante(EstanteModel.builder().idEstante(idEstante).codigo("E-" + idEstante).build())
                .pasillo(PasilloModel.builder().idPasillo(idPasillo).bodega(bodega).build())
                .ocupacionPct(ocupacion)
                .build();
    }

    // ── asignarUbicacion ─────────────────────────────────────────────────────

    @Test
    void asignarUbicacion_eligeLaDeMenorOcupacionYSumaUnoPorCiento() {
        BodegaModel central = bodega(1L, "Central");
        EstPasiModel llena = ubicacion(10L, 100L, central, 80.0);
        EstPasiModel libre = ubicacion(11L, 101L, central, 5.0);
        when(estPasiRepository.findByHabilitadaTrue()).thenReturn(List.of(llena, libre));
        UUID productoId = UUID.randomUUID();

        ProductoUbicacionAsignadaRabbitEvent resultado = service.asignarUbicacion(productoId, "Casco");

        assertEquals(productoId, resultado.getProductoId());
        assertEquals(1L, resultado.getIdBodega());
        assertEquals(101L, resultado.getIdPasillo());
        assertEquals(11L, resultado.getIdEstante());
        assertEquals(6.0, libre.getOcupacionPct());
        assertEquals(80.0, llena.getOcupacionPct());
        verify(estPasiRepository).save(libre);
    }

    @Test
    void asignarUbicacion_ocupacionNulaCuentaComoCero() {
        EstPasiModel sinDato = ubicacion(10L, 100L, bodega(1L, "Central"), null);
        when(estPasiRepository.findByHabilitadaTrue()).thenReturn(List.of(sinDato));

        service.asignarUbicacion(UUID.randomUUID(), "X");

        assertEquals(1.0, sinDato.getOcupacionPct());
    }

    @Test
    void asignarUbicacion_nuncaSuperaElCienPorCiento() {
        EstPasiModel casiLlena = ubicacion(10L, 100L, bodega(1L, "Central"), 99.5);
        when(estPasiRepository.findByHabilitadaTrue()).thenReturn(List.of(casiLlena));

        service.asignarUbicacion(UUID.randomUUID(), "X");

        assertEquals(100.0, casiLlena.getOcupacionPct());
    }

    @Test
    void asignarUbicacion_sinUbicacionesHabilitadas_lanzaSinCapacidad() {
        when(estPasiRepository.findByHabilitadaTrue()).thenReturn(List.of());

        assertThrows(SinCapacidadException.class, () -> service.asignarUbicacion(UUID.randomUUID(), "X"));

        verify(estPasiRepository, never()).save(any());
    }

    // ── elegirBodegaDespacho ─────────────────────────────────────────────────

    @Test
    void elegirBodegaDespacho_eligeLaDeMenorOcupacionPromedio() {
        BodegaModel central = bodega(1L, "Central");
        BodegaModel norte = bodega(2L, "Norte");
        when(bodegaRepository.findByActivaTrue()).thenReturn(List.of(central, norte));
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(1L)).thenReturn(70.0);
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(2L)).thenReturn(20.0);

        OrdenBodegaAsignadaRabbitEvent resultado = service.elegirBodegaDespacho(55L);

        assertEquals(55L, resultado.getOrdenId());
        assertEquals(2L, resultado.getIdBodega());
        assertEquals("Norte", resultado.getNombreBodega());
    }

    @Test
    void elegirBodegaDespacho_promedioNuloCuentaComoCero() {
        BodegaModel central = bodega(1L, "Central");
        BodegaModel vacia = bodega(2L, "Vacía");
        when(bodegaRepository.findByActivaTrue()).thenReturn(List.of(central, vacia));
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(1L)).thenReturn(10.0);
        when(estPasiRepository.calcularOcupacionPromedioPorBodega(2L)).thenReturn(null);

        assertEquals(2L, service.elegirBodegaDespacho(1L).getIdBodega());
    }

    @Test
    void elegirBodegaDespacho_sinBodegasActivas_lanzaSinCapacidad() {
        when(bodegaRepository.findByActivaTrue()).thenReturn(List.of());

        assertThrows(SinCapacidadException.class, () -> service.elegirBodegaDespacho(1L));
    }
}
