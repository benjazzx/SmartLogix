package Inventario.example.Inventario.config;

import Inventario.example.Inventario.model.BodegaModel;
import Inventario.example.Inventario.model.EstPasiModel;
import Inventario.example.Inventario.model.EstanteModel;
import Inventario.example.Inventario.model.PasilloModel;
import Inventario.example.Inventario.repository.BodegaRepository;
import Inventario.example.Inventario.repository.EstPasiRepository;
import Inventario.example.Inventario.repository.EstanteRepository;
import Inventario.example.Inventario.repository.PasilloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BodegaRepository bodegaRepository;
    private final PasilloRepository pasilloRepository;
    private final EstanteRepository estanteRepository;
    private final EstPasiRepository estPasiRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (bodegaRepository.count() > 0) {
            log.info("Inventario ya inicializado — omitiendo seed");
            return;
        }
        log.info("Inicializando datos de Inventario...");
        seedBodegas();
        log.info("Inventario inicializado correctamente");
    }

    private void seedBodegas() {
        // ── Bodega 1: Central Santiago ────────────────────────────────────────
        BodegaModel bodegaCentral = bodegaRepository.save(BodegaModel.builder()
                .nombre("Bodega Central Santiago")
                .direccion("Av. Américo Vespucio 1234")
                .ciudad("Santiago")
                .pais("Chile")
                .capacidadTotal(5000.0)
                .activa(true)
                .build());

        PasilloModel pasilloA1 = pasilloRepository.save(PasilloModel.builder()
                .codigo("PAS-A1")
                .descripcion("Pasillo principal — productos generales")
                .numeroOrden(1)
                .activo(true)
                .bodega(bodegaCentral)
                .build());

        PasilloModel pasilloB1 = pasilloRepository.save(PasilloModel.builder()
                .codigo("PAS-B1")
                .descripcion("Pasillo secundario — productos refrigerados")
                .numeroOrden(2)
                .activo(true)
                .bodega(bodegaCentral)
                .build());

        EstanteModel estante001 = estanteRepository.save(EstanteModel.builder()
                .codigo("EST-001")
                .descripcion("Estante metálico nivel alto")
                .numNiveles(5)
                .capacidadPorNivel(200.0)
                .activo(true)
                .build());

        EstanteModel estante002 = estanteRepository.save(EstanteModel.builder()
                .codigo("EST-002")
                .descripcion("Estante metálico nivel medio")
                .numNiveles(4)
                .capacidadPorNivel(150.0)
                .activo(true)
                .build());

        EstanteModel estante003 = estanteRepository.save(EstanteModel.builder()
                .codigo("EST-003")
                .descripcion("Estante refrigerado")
                .numNiveles(3)
                .capacidadPorNivel(100.0)
                .activo(true)
                .build());

        estPasiRepository.save(EstPasiModel.builder()
                .estante(estante001)
                .pasillo(pasilloA1)
                .posicion("Lado izquierdo")
                .numeroFila(1)
                .ocupacionPct(35.0)
                .habilitada(true)
                .observaciones("Zona A — carga regular")
                .build());

        estPasiRepository.save(EstPasiModel.builder()
                .estante(estante002)
                .pasillo(pasilloA1)
                .posicion("Lado derecho")
                .numeroFila(2)
                .ocupacionPct(60.0)
                .habilitada(true)
                .observaciones("Zona A — alta rotación")
                .build());

        estPasiRepository.save(EstPasiModel.builder()
                .estante(estante003)
                .pasillo(pasilloB1)
                .posicion("Centro")
                .numeroFila(1)
                .ocupacionPct(20.0)
                .habilitada(true)
                .observaciones("Zona refrigerada — temperatura controlada")
                .build());

        log.info("Bodega Central Santiago creada con 2 pasillos y 3 estantes");

        // ── Bodega 2: Norte Logística ─────────────────────────────────────────
        BodegaModel bodegaNorte = bodegaRepository.save(BodegaModel.builder()
                .nombre("Bodega Norte Logística")
                .direccion("Ruta 5 Norte Km 12")
                .ciudad("Antofagasta")
                .pais("Chile")
                .capacidadTotal(3000.0)
                .activa(true)
                .build());

        PasilloModel pasilloC1 = pasilloRepository.save(PasilloModel.builder()
                .codigo("PAS-C1")
                .descripcion("Pasillo norte — carga pesada")
                .numeroOrden(1)
                .activo(true)
                .bodega(bodegaNorte)
                .build());

        EstanteModel estante004 = estanteRepository.save(EstanteModel.builder()
                .codigo("EST-004")
                .descripcion("Estante industrial carga pesada")
                .numNiveles(3)
                .capacidadPorNivel(500.0)
                .activo(true)
                .build());

        EstanteModel estante005 = estanteRepository.save(EstanteModel.builder()
                .codigo("EST-005")
                .descripcion("Estante mediano multipropósito")
                .numNiveles(4)
                .capacidadPorNivel(250.0)
                .activo(true)
                .build());

        estPasiRepository.save(EstPasiModel.builder()
                .estante(estante004)
                .pasillo(pasilloC1)
                .posicion("Lado izquierdo")
                .numeroFila(1)
                .ocupacionPct(80.0)
                .habilitada(true)
                .observaciones("Alta ocupación — revisar capacidad")
                .build());

        estPasiRepository.save(EstPasiModel.builder()
                .estante(estante005)
                .pasillo(pasilloC1)
                .posicion("Lado derecho")
                .numeroFila(2)
                .ocupacionPct(45.0)
                .habilitada(true)
                .build());

        log.info("Bodega Norte Logística creada con 1 pasillo y 2 estantes");
    }
}
