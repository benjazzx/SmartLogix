package Inventario.example.Inventario.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuracion centralizada de RabbitMQ (EP3 DSY1107): un exchange de topico para el
// dominio "logistica", con una cola y su DLQ por cada caso de uso (producto creado,
// orden creada). Todos los nombres vienen de application.properties, nunca
// hardcodeados en RabbitConfig ni en los consumidores/productores.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.dlx}")
    private String dlxName;

    @Value("${rabbitmq.logistica.producto-creado.queue}")
    private String productoQueueName;

    @Value("${rabbitmq.logistica.producto-creado.routing-key}")
    private String productoRoutingKey;

    @Value("${rabbitmq.logistica.producto-creado.dlq}")
    private String productoDlqName;

    @Value("${rabbitmq.logistica.producto-creado.dlq-routing-key}")
    private String productoDlqRoutingKey;

    @Value("${rabbitmq.logistica.orden-creada.queue}")
    private String ordenQueueName;

    @Value("${rabbitmq.logistica.orden-creada.routing-key}")
    private String ordenRoutingKey;

    @Value("${rabbitmq.logistica.orden-creada.dlq}")
    private String ordenDlqName;

    @Value("${rabbitmq.logistica.orden-creada.dlq-routing-key}")
    private String ordenDlqRoutingKey;

    @Bean
    public TopicExchange logisticaExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange logisticaDlx() {
        return new TopicExchange(dlxName, true, false);
    }

    // ── Caso de uso 1: producto creado → asignar ubicacion (bodega/pasillo/estante) ──

    @Bean
    public Queue productoUbicacionQueue() {
        return QueueBuilder.durable(productoQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", productoDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue productoUbicacionDlq() {
        return QueueBuilder.durable(productoDlqName).build();
    }

    @Bean
    public Binding productoUbicacionBinding() {
        return BindingBuilder.bind(productoUbicacionQueue()).to(logisticaExchange()).with(productoRoutingKey);
    }

    @Bean
    public Binding productoUbicacionDlqBinding() {
        return BindingBuilder.bind(productoUbicacionDlq()).to(logisticaDlx()).with(productoDlqRoutingKey);
    }

    // ── Caso de uso 2: orden creada → asignar bodega de despacho ──────────────────

    @Bean
    public Queue ordenBodegaQueue() {
        return QueueBuilder.durable(ordenQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", ordenDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue ordenBodegaDlq() {
        return QueueBuilder.durable(ordenDlqName).build();
    }

    @Bean
    public Binding ordenBodegaBinding() {
        return BindingBuilder.bind(ordenBodegaQueue()).to(logisticaExchange()).with(ordenRoutingKey);
    }

    @Bean
    public Binding ordenBodegaDlqBinding() {
        return BindingBuilder.bind(ordenBodegaDlq()).to(logisticaDlx()).with(ordenDlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
