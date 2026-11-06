package Orden.example.Orden.config;

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

// Topología RabbitMQ de Orden. Los nombres vienen de application.properties.
//  - Productor: publica orden.creada en el exchange de logística (lo consume Inventario).
//  - Consumidor: recibe orden.bodega-asignada (respuesta de Inventario) en su propia cola,
//    con DLQ para los mensajes que no se pueden procesar.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.dlx}")
    private String dlxName;

    @Value("${rabbitmq.logistica.orden-bodega-asignada.queue}")
    private String bodegaAsignadaQueueName;

    @Value("${rabbitmq.logistica.orden-bodega-asignada.routing-key}")
    private String bodegaAsignadaRoutingKey;

    @Value("${rabbitmq.logistica.orden-bodega-asignada.dlq}")
    private String bodegaAsignadaDlqName;

    @Value("${rabbitmq.logistica.orden-bodega-asignada.dlq-routing-key}")
    private String bodegaAsignadaDlqRoutingKey;

    @Bean
    public TopicExchange logisticaExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange logisticaDlx() {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue bodegaAsignadaQueue() {
        return QueueBuilder.durable(bodegaAsignadaQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", bodegaAsignadaDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue bodegaAsignadaDlq() {
        return QueueBuilder.durable(bodegaAsignadaDlqName).build();
    }

    @Bean
    public Binding bodegaAsignadaBinding() {
        return BindingBuilder.bind(bodegaAsignadaQueue()).to(logisticaExchange()).with(bodegaAsignadaRoutingKey);
    }

    @Bean
    public Binding bodegaAsignadaDlqBinding() {
        return BindingBuilder.bind(bodegaAsignadaDlq()).to(logisticaDlx()).with(bodegaAsignadaDlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
