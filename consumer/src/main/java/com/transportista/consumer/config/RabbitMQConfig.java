package com.transportista.consumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el microservicio Consumer.
 *
 * <p>Declara las mismas colas y exchange que el Producer para garantizar
 * que las estructuras existen en RabbitMQ cuando este servicio inicia.
 * El listener {@link com.transportista.consumer.listener.GuiaMensajeConsumer}
 * escucha {@code guias.queue} automáticamente.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_PRINCIPAL   = "guias.queue";
    public static final String QUEUE_ERROR       = "guias.error.queue";
    public static final String EXCHANGE_NAME     = "guias.exchange";
    public static final String ROUTING_KEY_OK    = "guias.routing.key";
    public static final String ROUTING_KEY_ERROR = "guias.error.routing.key";

    @Bean
    public Queue queuePrincipal() {
        return QueueBuilder.durable(QUEUE_PRINCIPAL).build();
    }

    @Bean
    public Queue queueError() {
        return QueueBuilder.durable(QUEUE_ERROR).build();
    }

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding bindingPrincipal(Queue queuePrincipal, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queuePrincipal).to(guiasExchange).with(ROUTING_KEY_OK);
    }

    @Bean
    public Binding bindingError(Queue queueError, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queueError).to(guiasExchange).with(ROUTING_KEY_ERROR);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
