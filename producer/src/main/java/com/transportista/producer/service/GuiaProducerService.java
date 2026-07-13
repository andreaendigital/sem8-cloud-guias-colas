package com.transportista.producer.service;

import com.transportista.producer.config.RabbitMQConfig;
import com.transportista.producer.dto.GuiaMensajeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de publicación de mensajes de Guías en RabbitMQ.
 *
 * <p>Intenta publicar en {@code guias.queue} (cola-1). Si ocurre una excepción AMQP,
 * reencamina automáticamente el mensaje a {@code guias.error.queue} (cola-2 / DLQ).</p>
 */
@Service
public class GuiaProducerService {

    private static final Logger log = LoggerFactory.getLogger(GuiaProducerService.class);

    private final RabbitTemplate rabbitTemplate;

    public GuiaProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un mensaje en la cola principal {@code guias.queue}.
     *
     * <p>Si la publicación falla, el mensaje se reenvía a {@code guias.error.queue}
     * para su posterior inspección o reprocesamiento.</p>
     *
     * @param mensaje DTO con los datos de la guía a publicar
     * @return {@code true} si fue a la cola principal, {@code false} si fue a la DLQ
     */
    public boolean publicar(GuiaMensajeDTO mensaje) {
        // Asegurar que el mensaje tenga timestamp y guiaId si no vienen del request
        if (mensaje.getFechaMensaje() == null) {
            mensaje.setFechaMensaje(Instant.now());
        }
        if (mensaje.getGuiaId() == null) {
            mensaje.setGuiaId(UUID.randomUUID());
        }

        try {
            log.info("[PRODUCER] Publicando mensaje en cola principal — guiaId={}, operacion={}",
                     mensaje.getGuiaId(), mensaje.getOperacion());

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_OK,
                mensaje
            );

            log.info("[PRODUCER] Mensaje publicado exitosamente en {} — guiaId={}",
                     RabbitMQConfig.QUEUE_PRINCIPAL, mensaje.getGuiaId());
            return true;

        } catch (AmqpException ex) {
            log.error("[PRODUCER] Fallo al publicar en cola principal. Redirigiendo a DLQ. " +
                      "guiaId={}, error={}", mensaje.getGuiaId(), ex.getMessage());
            enviarADlq(mensaje);
            return false;
        }
    }

    /**
     * Reenvía el mensaje a la Dead Letter Queue {@code guias.error.queue}.
     *
     * @param mensaje DTO que no pudo ser procesado en la cola principal
     */
    private void enviarADlq(GuiaMensajeDTO mensaje) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_ERROR,
                mensaje
            );
            log.warn("[PRODUCER] Mensaje redirigido a DLQ ({}) — guiaId={}",
                     RabbitMQConfig.QUEUE_ERROR, mensaje.getGuiaId());
        } catch (AmqpException dlqEx) {
            log.error("[PRODUCER] Fallo crítico: no se pudo enviar a la DLQ. guiaId={}, error={}",
                      mensaje.getGuiaId(), dlqEx.getMessage());
        }
    }
}
