package Producto.example.Producto.messaging;

import Producto.example.Producto.dto.ProductoActualizadoEvent;
import Producto.example.Producto.dto.ProductoUbicacionChangedEvent;
import Producto.example.Producto.model.ProductoModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoEventProducer {

    private final StreamBridge streamBridge;

    public void publishProductoActualizado(ProductoModel p, ProductoActualizadoEvent.TipoEvento tipo) {
        ProductoActualizadoEvent event = new ProductoActualizadoEvent(
                p.getId(),
                p.getNombre(),
                p.getPrecio(),
                p.getStock(),
                p.getCategoria().getNombre(),
                p.getEstadoNombre(),
                p.getActivo(),
                tipo
        );
        streamBridge.send("producto-actualizado-topic", event);
        log.info("[PRODUCER] producto-actualizado-topic → productoId={} tipo={} stock={}",
                p.getId(), tipo, p.getStock());
    }

    public void publishUbicacionChanged(Long idEstante, int delta) {
        ProductoUbicacionChangedEvent event = new ProductoUbicacionChangedEvent(
                UUID.randomUUID().toString(), idEstante, delta);
        streamBridge.send("producto-ubicacion-changed-topic", event);
        log.info("[PRODUCER] producto-ubicacion-changed-topic → idEstante={} delta={}", idEstante, delta);
    }
}
