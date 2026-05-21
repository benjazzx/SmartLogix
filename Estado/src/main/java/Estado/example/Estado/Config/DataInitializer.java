package Estado.example.Estado.Config;

import Estado.example.Estado.Model.Estado;
import Estado.example.Estado.Model.TipoDeEstadoModel;
import Estado.example.Estado.Repository.EstadoRepository;
import Estado.example.Estado.Repository.TipoDeEstadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final TipoDeEstadoRepository tipoRepo;
    private final EstadoRepository estadoRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, String> cuentaEstados = new LinkedHashMap<>();
        cuentaEstados.put("activo",                   "Usuario activo en el sistema");
        cuentaEstados.put("inactivo",                 "Usuario desactivado del sistema");
        cuentaEstados.put("pendiente_verificacion",   "Cuenta pendiente de verificación");
        seedTipoAndEstados("cuenta", "Estados de cuenta de usuario", cuentaEstados);

        Map<String, String> laboralEstados = new LinkedHashMap<>();
        laboralEstados.put("disponible",    "Personal disponible para operar");
        laboralEstados.put("no_disponible", "Personal no disponible");
        seedTipoAndEstados("laboral", "Estados laborales del personal operativo", laboralEstados);

        Map<String, String> ordenEstados = new LinkedHashMap<>();
        ordenEstados.put("Pendiente",   "Orden registrada, pendiente de procesamiento");
        ordenEstados.put("Procesando",  "Orden en proceso de preparación");
        ordenEstados.put("Aprobado",    "Orden aprobada y lista para despacho");
        ordenEstados.put("En tránsito", "Orden en camino al destino");
        ordenEstados.put("Entregado",   "Orden entregada exitosamente al cliente");
        ordenEstados.put("Cancelado",   "Orden cancelada");
        seedTipoAndEstados("orden", "Estados del ciclo de vida de órdenes", ordenEstados);
    }

    private void seedTipoAndEstados(String tipoNombre, String tipoDesc, Map<String, String> estados) {
        TipoDeEstadoModel tipo = tipoRepo.findByNombre(tipoNombre).orElseGet(() -> {
            log.info("[DataInit] Creando TipoDeEstado: {}", tipoNombre);
            return tipoRepo.save(new TipoDeEstadoModel(null, tipoNombre, tipoDesc));
        });

        estados.forEach((nombre, desc) -> {
            if (estadoRepo.findByNombre(nombre).isEmpty()) {
                log.info("[DataInit] Creando Estado: {} (tipo={})", nombre, tipoNombre);
                estadoRepo.save(new Estado(null, nombre, desc, tipo));
            }
        });
    }
}
