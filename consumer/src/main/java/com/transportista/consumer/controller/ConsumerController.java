package com.transportista.consumer.controller;

import com.transportista.consumer.model.GuiaDespachoEvent;
import com.transportista.consumer.repository.GuiaDespachoEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST del microservicio Consumer.
 *
 * <p>Expone endpoints para consultar los eventos de Guías de Despacho
 * procesados desde RabbitMQ y persistidos en la tabla {@code GUIAS_EVENTOS}.</p>
 *
 * <p>El endpoint {@code GET /api/v1/consumer/procesar} permite verificar
 * que el Consumer está activo y muestra estadísticas de procesamiento,
 * tal como lo requiere el enunciado académico.</p>
 */
@RestController
@RequestMapping("/api/v1/consumer")
public class ConsumerController {

    private final GuiaDespachoEventRepository eventoRepository;

    public ConsumerController(GuiaDespachoEventRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/consumer/procesar — Estado del consumer y estadísticas
    // -------------------------------------------------------------------------

    /**
     * Verifica el estado del Consumer y devuelve estadísticas de procesamiento.
     *
     * <p>Este endpoint confirma que el Consumer está activo y escuchando
     * {@code guias.queue}. Devuelve conteos por tipo de operación y
     * el total de eventos procesados.</p>
     *
     * <p>Accesible para {@code admin} y {@code transportista}.</p>
     */
    @GetMapping("/procesar")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin') or " +
                  "authentication.principal.claims['extension_consultaRole'].equals('transportista')")
    public ResponseEntity<Map<String, Object>> estadoConsumer() {
        long totalEventos   = eventoRepository.count();
        long totalCrear     = eventoRepository.countByOperacion("CREAR");
        long totalActualizar = eventoRepository.countByOperacion("ACTUALIZAR");
        long totalEliminar  = eventoRepository.countByOperacion("ELIMINAR");

        return ResponseEntity.ok(Map.of(
            "status", "CONSUMER_ACTIVO",
            "cola_escuchada", "guias.queue",
            "total_eventos_procesados", totalEventos,
            "eventos_CREAR", totalCrear,
            "eventos_ACTUALIZAR", totalActualizar,
            "eventos_ELIMINAR", totalEliminar
        ));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/consumer/eventos — Listar todos los eventos (admin)
    // -------------------------------------------------------------------------

    /**
     * Lista todos los eventos de guías procesados con paginación.
     * Solo accesible para {@code admin}.
     *
     * @param page número de página (base 0)
     * @param size elementos por página (máx 100)
     */
    @GetMapping("/eventos")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<Page<GuiaDespachoEvent>> listarEventos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 100);
        Page<GuiaDespachoEvent> eventos = eventoRepository.findAll(
            PageRequest.of(page, safeSize, Sort.by("procesadoEn").descending())
        );
        return ResponseEntity.ok(eventos);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/consumer/eventos/{guiaId} — Eventos de una guía específica
    // -------------------------------------------------------------------------

    /**
     * Devuelve el historial de eventos de una guía específica por su UUID.
     * Accesible para {@code admin} y {@code transportista}.
     *
     * @param guiaId UUID de la guía de negocio
     */
    @GetMapping("/eventos/{guiaId}")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin') or " +
                  "authentication.principal.claims['extension_consultaRole'].equals('transportista')")
    public ResponseEntity<List<GuiaDespachoEvent>> eventosPorGuia(@PathVariable UUID guiaId) {
        List<GuiaDespachoEvent> eventos =
            eventoRepository.findByGuiaIdOrderByProcesadoEnDesc(guiaId);
        return ResponseEntity.ok(eventos);
    }
}
