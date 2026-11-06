package Producto.example.Producto.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Sin el MessageConverter, RabbitTemplate usa el conversor por defecto (serializacion
// nativa de Java, no JSON) y el mensaje llega ilegible al otro lado. Cada servicio que
// publica o consume necesita su propio bean — no se comparte entre JVMs/contextos de
// Spring. El TopicExchange se redeclara aqui con los mismos parametros que en Inventario
// (dueño de las colas/DLQ): la declaracion es idempotente, asi que no importa cual de
// los dos servicios arranque primero.
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.logistica.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange logisticaExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
