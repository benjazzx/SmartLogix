package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.BodegaActualizadaEvent;
import Producto.example.Producto.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

// Escucha cambios de bodega publicados por Inventario. Cuando una bodega se
// desactiva, los productos que tenían esa bodega asignada quedan sin ubicación
// y deben reasignarse manualmente a una bodega activa.
@Slf4j
@Component
@RequiredArgsConstructor
public class BodegaEventConsumer {

    private final ProductoService productoService;

    @Bean
    public Consumer<BodegaActualizadaEvent> onBodegaActualizada() {
        return event -> {
            try {
                if (event == null || event.getIdBodega() == null) return;
                if ("DESACTIVADA".equals(event.getTipoEvento())) {
                    log.info("[Producto] Bodega desactivada — idBodega={}, liberando ubicación de sus productos",
                            event.getIdBodega());
                    productoService.liberarUbicacionPorBodega(event.getIdBodega());
                }
            } catch (Exception e) {
                log.error("[Producto] Error procesando onBodegaActualizada — idBodega={}: {}",
                        event != null ? event.getIdBodega() : null, e.getMessage(), e);
            }
        };
    }
}
