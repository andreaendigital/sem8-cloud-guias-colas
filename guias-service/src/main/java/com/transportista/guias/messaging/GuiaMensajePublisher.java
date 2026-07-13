package com.transportista.guias.messaging;

import com.transportista.guias.config.RabbitMQConfig;
import com.transportista.guias.dto.GuiaMensajeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Componente de publicación de mensajes de Guías en RabbitMQ.
 *
 * <p>Utilizado por {@link com.transportista.guias.controller.GuiaController} para
 * publicar eventos de creación, actualización y eliminación de guías en
 * {@code guias.queue} (cola-1). Ante fallo AMQP, reencamina a
 * {@code guias.error.queue} (cola-2 / DLQ).</p>
 */
@Component
public class GuiaMensajePublisher {

    private static final Logger log = LoggerFactory.getLogger(GuiaMensajePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public GuiaMensajePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un mensaje en la cola principal {@code guias.queue}.
     *
     * <p>Si falla, el mensaje es redirigido automáticamente a la DLQ
     * {@code guias.error.queue} para no perder el evento.</p>
     *
     * @param mensaje DTO con los datos del evento de guía
     */
    public void publicar(GuiaMensajeDTO mensaje) {
        if (mensaje.getFechaMensaje() == null) {
            mensaje.setFechaMensaje(Instant.now());
        }

        try {
            log.info("[GUIAS-SERVICE] Publicando en cola principal — guiaId={}, operacion={}",
                     mensaje.getGuiaId(), mensaje.getOperacion());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_OK,
                mensaje
            );

            log.info("[GUIAS-SERVICE] Mensaje publicado exitosamente en {} — guiaId={}",
                     RabbitMQConfig.QUEUE_PRINCIPAL, mensaje.getGuiaId());

        } catch (AmqpException ex) {
            log.error("[GUIAS-SERVICE] Fallo al publicar en cola principal. Redirigiendo a DLQ. " +
                      "guiaId={}, error={}", mensaje.getGuiaId(), ex.getMessage());
            enviarADlq(mensaje);
        }
    }

    private void enviarADlq(GuiaMensajeDTO mensaje) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_ERROR,
                mensaje
            );
            log.warn("[GUIAS-SERVICE] Mensaje redirigido a DLQ ({}) — guiaId={}",
                     RabbitMQConfig.QUEUE_ERROR, mensaje.getGuiaId());
        } catch (AmqpException dlqEx) {
            log.error("[GUIAS-SERVICE] Fallo crítico: no se pudo enviar a la DLQ. guiaId={}, error={}",
                      mensaje.getGuiaId(), dlqEx.getMessage());
        }
    }
}
