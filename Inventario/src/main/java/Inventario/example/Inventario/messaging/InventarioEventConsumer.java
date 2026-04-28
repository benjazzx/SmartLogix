package Inventario.example.Inventario.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Consumidor Kafka orientado a eventos del ecosistema SmartLogix.
 * Escucha el tópico "orden-creada-topic" para reaccionar ante nuevas órdenes
 * y potencialmente ajustar la disponibilidad de espacio en inventario.
 */
@Slf4j
@Component
public class InventarioEventConsumer {

    /**
     * Consume eventos de órdenes creadas publicados por el microservicio Orden.
     * Binding: Spring Cloud Stream → función "onOrdenCreada" → tópico "orden-creada-topic"
     */
    @Bean
    public Consumer<Map<String, Object>> onOrdenCreada() {
        return event -> {
            try {
                Object ordenId = event.get("ordenId");
                Object userId  = event.get("userId");
                log.info("[Inventario CONSUMER] Orden creada recibida — ordenId={} userId={}",
                        ordenId, userId);
                // Lógica futura: reservar espacio, actualizar disponibilidad, auditoría, etc.
            } catch (Exception e) {
                log.error("[Inventario CONSUMER] Error procesando onOrdenCreada: {}", e.getMessage(), e);
            }
        };
    }
}
