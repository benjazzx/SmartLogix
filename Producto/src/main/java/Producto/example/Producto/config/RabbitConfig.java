package Producto.example.Producto.config;

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

// Topología RabbitMQ de Producto. Los nombres vienen de application.properties.
//  - Productor: publica producto.creado en el exchange de logística (lo consume Inventario).
//  - Consumidor: recibe producto.ubicacion-asignada (respuesta de Inventario) en su propia cola,
//    con DLQ para los mensajes que no se pueden procesar.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.logistica.dlx}")
    private String dlxName;

    @Value("${rabbitmq.logistica.producto-ubicacion-asignada.queue}")
    private String ubicacionAsignadaQueueName;

    @Value("${rabbitmq.logistica.producto-ubicacion-asignada.routing-key}")
    private String ubicacionAsignadaRoutingKey;

    @Value("${rabbitmq.logistica.producto-ubicacion-asignada.dlq}")
    private String ubicacionAsignadaDlqName;

    @Value("${rabbitmq.logistica.producto-ubicacion-asignada.dlq-routing-key}")
    private String ubicacionAsignadaDlqRoutingKey;

    @Bean
    public TopicExchange logisticaExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange logisticaDlx() {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue ubicacionAsignadaQueue() {
        return QueueBuilder.durable(ubicacionAsignadaQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", ubicacionAsignadaDlqRoutingKey)
                .build();
    }

    @Bean
    public Queue ubicacionAsignadaDlq() {
        return QueueBuilder.durable(ubicacionAsignadaDlqName).build();
    }

    @Bean
    public Binding ubicacionAsignadaBinding() {
        return BindingBuilder.bind(ubicacionAsignadaQueue()).to(logisticaExchange()).with(ubicacionAsignadaRoutingKey);
    }

    @Bean
    public Binding ubicacionAsignadaDlqBinding() {
        return BindingBuilder.bind(ubicacionAsignadaDlq()).to(logisticaDlx()).with(ubicacionAsignadaDlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
