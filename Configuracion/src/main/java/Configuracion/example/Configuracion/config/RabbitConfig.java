package Configuracion.example.Configuracion.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Topología RabbitMQ de Configuracion (consumidor). Los nombres vienen de application.properties.
// Cola propia enlazada al exchange de usuarios, con DLQ para los mensajes que no se pueden procesar.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.usuarios.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.usuarios.dlx}")
    private String dlxName;

    @Value("${rabbitmq.usuarios.registrado.queue}")
    private String queueName;

    @Value("${rabbitmq.usuarios.registrado.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.usuarios.registrado.dlq}")
    private String dlqName;

    @Value("${rabbitmq.usuarios.registrado.dlq-routing-key}")
    private String dlqRoutingKey;

    @Bean
    public TopicExchange usuariosExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange usuariosDlx() {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue usuarioRegistradoQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Queue usuarioRegistradoDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding usuarioRegistradoBinding() {
        return BindingBuilder.bind(usuarioRegistradoQueue()).to(usuariosExchange()).with(routingKey);
    }

    @Bean
    public Binding usuarioRegistradoDlqBinding() {
        return BindingBuilder.bind(usuarioRegistradoDlq()).to(usuariosDlx()).with(dlqRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
