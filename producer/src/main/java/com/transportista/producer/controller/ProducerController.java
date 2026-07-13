package com.transportista.producer.controller;

import com.transportista.producer.dto.GuiaMensajeDTO;
import com.transportista.producer.dto.PublicarMensajeRequestDTO;
import com.transportista.producer.service.GuiaProducerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST del microservicio Producer.
 *
 * <p>Expone {@code POST /api/v1/producer/publicar} para recibir datos de una guía
 * y publicarlos como mensaje JSON en RabbitMQ. Solo el rol {@code admin} puede
 * invocar este endpoint.</p>
 */
@RestController
@RequestMapping("/api/v1/producer")
@Validated
public class ProducerController {

    private final GuiaProducerService producerService;

    public ProducerController(GuiaProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * Publica un mensaje de Guía de Despacho en RabbitMQ.
     *
     * <p>El mensaje va a {@code guias.queue} (cola-1). Si falla, se reenvía
     * automáticamente a {@code guias.error.queue} (cola-2 / DLQ).</p>
     *
     * @param request datos de la guía y operación a publicar
     * @return 200 OK con el guiaId y la cola de destino, o 202 si fue a la DLQ
     */
    @PostMapping("/publicar")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<Map<String, Object>> publicar(
            @Valid @RequestBody PublicarMensajeRequestDTO request) {

        // Construir el mensaje para RabbitMQ
        UUID guiaId = request.getGuiaId() != null ? request.getGuiaId() : UUID.randomUUID();

        GuiaMensajeDTO mensaje = new GuiaMensajeDTO(
            guiaId,
            request.getTransportistaId(),
            request.getFechaEnvio(),
            request.getDestinatario(),
            request.getDireccionDestino(),
            request.getPesoKg(),
            request.getDescripcionCarga(),
            request.getObservaciones(),
            request.getOperacion(),
            Instant.now()
        );

        boolean enviado = producerService.publicar(mensaje);

        if (enviado) {
            return ResponseEntity.ok(Map.of(
                "status", "PUBLICADO",
                "cola", "guias.queue",
                "guiaId", guiaId.toString(),
                "operacion", mensaje.getOperacion().name(),
                "fechaMensaje", mensaje.getFechaMensaje().toString()
            ));
        } else {
            // Mensaje fue a la DLQ (cola de error)
            return ResponseEntity.accepted().body(Map.of(
                "status", "ENVIADO_A_DLQ",
                "cola", "guias.error.queue",
                "guiaId", guiaId.toString(),
                "operacion", mensaje.getOperacion().name(),
                "fechaMensaje", mensaje.getFechaMensaje().toString()
            ));
        }
    }
}
