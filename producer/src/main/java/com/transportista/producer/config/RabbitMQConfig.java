package com.transportista.producer.config;

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
 * Configuración de RabbitMQ para el microservicio Producer.
 *
 * <p>Declara dos colas, un exchange directo y sus bindings:</p>
 * <ul>
 *   <li>{@code guias.queue}       — cola principal (cola-1)</li>
 *   <li>{@code guias.error.queue} — Dead Letter Queue (cola-2)</li>
 * </ul>
 *
 * <p>Los mensajes se serializan como JSON usando {@link Jackson2JsonMessageConverter}.</p>
 */
@Configuration
public class RabbitMQConfig {

    // -------------------------------------------------------------------------
    // Constantes de nombres (compartidas con el Consumer)
    // -------------------------------------------------------------------------
    public static final String QUEUE_PRINCIPAL   = "guias.queue";
    public static final String QUEUE_ERROR       = "guias.error.queue";
    public static final String EXCHANGE_NAME     = "guias.exchange";
    public static final String ROUTING_KEY_OK    = "guias.routing.key";
    public static final String ROUTING_KEY_ERROR = "guias.error.routing.key";

    // -------------------------------------------------------------------------
    // Cola principal — durable para sobrevivir reinicios de RabbitMQ
    // -------------------------------------------------------------------------
    @Bean
    public Queue queuePrincipal() {
        return QueueBuilder.durable(QUEUE_PRINCIPAL).build();
    }

    // -------------------------------------------------------------------------
    // Dead Letter Queue (DLQ) — recibe mensajes que fallaron en cola-1
    // -------------------------------------------------------------------------
    @Bean
    public Queue queueError() {
        return QueueBuilder.durable(QUEUE_ERROR).build();
    }

    // -------------------------------------------------------------------------
    // Direct Exchange — enruta por routing key exacta
    // -------------------------------------------------------------------------
    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // -------------------------------------------------------------------------
    // Binding: exchange → cola principal
    // -------------------------------------------------------------------------
    @Bean
    public Binding bindingPrincipal(Queue queuePrincipal, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queuePrincipal)
                .to(guiasExchange)
                .with(ROUTING_KEY_OK);
    }

    // -------------------------------------------------------------------------
    // Binding: exchange → cola de error (DLQ)
    // -------------------------------------------------------------------------
    @Bean
    public Binding bindingError(Queue queueError, DirectExchange guiasExchange) {
        return BindingBuilder.bind(queueError)
                .to(guiasExchange)
                .with(ROUTING_KEY_ERROR);
    }

    // -------------------------------------------------------------------------
    // Convertidor JSON para mensajes AMQP
    // -------------------------------------------------------------------------
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // -------------------------------------------------------------------------
    // RabbitTemplate con convertidor JSON configurado
    // -------------------------------------------------------------------------
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
