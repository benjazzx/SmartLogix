package Inventario.example.Inventario.messaging;

import Inventario.example.Inventario.dto.BodegaActualizadaEvent;
import Inventario.example.Inventario.dto.UbicacionActualizadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventarioEventProducer {

    @Autowired
    private StreamBridge streamBridge;

    /**
     * Publica un evento cuando se crea, actualiza o elimina una asignación estante-pasillo.
     * Tópico consumido por: Envío (para saber ubicaciones disponibles).
     */
    public void publishUbicacionActualizada(UbicacionActualizadaEvent event) {
        boolean sent = streamBridge.send("ubicacion-actualizada-topic", event);
        log.info("[Inventario PRODUCER] Evento '{}' publicado en ubicacion-actualizada-topic para estante={} pasillo={} → {}",
                event.getTipoEvento(), event.getIdEstante(), event.getIdPasillo(), sent);
    }

    /**
     * Publica un evento cuando se crea, actualiza o desactiva una bodega.
     * Tópico consumido por: Envío (para conocer bodegas de origen/destino).
     */
    public void publishBodegaActualizada(BodegaActualizadaEvent event) {
        boolean sent = streamBridge.send("bodega-actualizada-topic", event);
        log.info("[Inventario PRODUCER] Evento '{}' publicado en bodega-actualizada-topic para bodega={} → {}",
                event.getTipoEvento(), event.getIdBodega(), sent);
    }
}
