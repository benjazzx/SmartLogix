package User.example.Users.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Topología RabbitMQ de Users (productor). Los nombres vienen de application.properties.
// Las colas las declara cada consumidor (Configuracion) junto con su DLQ.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.usuarios.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange usuariosExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
