package com.transportista.consumer.listener;

import com.transportista.consumer.dto.GuiaMensajeDTO;
import com.transportista.consumer.model.GuiaDespachoEvent;
import com.transportista.consumer.repository.GuiaDespachoEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener de mensajes de Guías de Despacho desde RabbitMQ.
 *
 * <p>Escucha la cola {@code guias.queue} (cola-1) y persiste cada mensaje
 * recibido como un {@link GuiaDespachoEvent} en la tabla {@code GUIAS_EVENTOS}.</p>
 *
 * <p>Si la persistencia falla, la excepción no es silenciada para que Spring AMQP
 * pueda aplicar la política de reintentos / NACK configurada en el broker.</p>
 */
@Component
public class GuiaMensajeConsumer {

    private static final Logger log = LoggerFactory.getLogger(GuiaMensajeConsumer.class);

    private final GuiaDespachoEventRepository eventoRepository;

    public GuiaMensajeConsumer(GuiaDespachoEventRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * Procesa un mensaje de la cola principal {@code guias.queue}.
     *
     * <p>Convierte el {@link GuiaMensajeDTO} en un {@link GuiaDespachoEvent}
     * y lo persiste en la base de datos del Consumer.</p>
     *
     * @param mensaje DTO deserializado automáticamente desde JSON por Spring AMQP
     */
    @RabbitListener(queues = "guias.queue")
    public void procesarMensaje(GuiaMensajeDTO mensaje) {
        log.info("[CONSUMER] Mensaje recibido de guias.queue — guiaId={}, operacion={}",
                 mensaje.getGuiaId(), mensaje.getOperacion());

        try {
            GuiaDespachoEvent evento = new GuiaDespachoEvent();
            evento.setGuiaId(mensaje.getGuiaId());
            evento.setTransportistaId(mensaje.getTransportistaId());
            evento.setFechaEnvio(mensaje.getFechaEnvio());
            evento.setDestinatario(mensaje.getDestinatario());
            evento.setDireccionDestino(mensaje.getDireccionDestino());
            evento.setPesoKg(mensaje.getPesoKg());
            evento.setDescripcionCarga(mensaje.getDescripcionCarga());
            evento.setObservaciones(mensaje.getObservaciones());
            evento.setOperacion(
                mensaje.getOperacion() != null ? mensaje.getOperacion().name() : "DESCONOCIDO"
            );
            evento.setFechaMensaje(mensaje.getFechaMensaje());

            GuiaDespachoEvent guardado = eventoRepository.save(evento);

            log.info("[CONSUMER] Evento persistido en GUIAS_EVENTOS — id={}, guiaId={}, operacion={}",
                     guardado.getId(), guardado.getGuiaId(), guardado.getOperacion());

        } catch (Exception ex) {
            log.error("[CONSUMER] Error al persistir evento de guía. guiaId={}, error={}",
                      mensaje.getGuiaId(), ex.getMessage(), ex);
            // Relanzar para que AMQP haga NACK y el mensaje vuelva a la cola o vaya a la DLQ
            throw new RuntimeException("Error al persistir evento de guía: " + ex.getMessage(), ex);
        }
    }
}
