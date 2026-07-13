package com.transportista.guias.controller;

import com.transportista.guias.dto.ActualizarGuiaRequestDTO;
import com.transportista.guias.dto.CrearGuiaRequestDTO;
import com.transportista.guias.dto.GuiaListItemDTO;
import com.transportista.guias.dto.GuiaMensajeDTO;
import com.transportista.guias.dto.GuiaResponseDTO;
import com.transportista.guias.dto.PaginatedResponseDTO;
import com.transportista.guias.messaging.GuiaMensajePublisher;
import com.transportista.guias.service.GuiaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Controlador REST para la gestión del ciclo de vida de las Guías de Despacho.
 *
 * <p>Seguridad basada en OAuth2 / Azure AD B2C con claim {@code extension_consultaRole}:</p>
 * <ul>
 *   <li>{@code admin} — acceso total a todos los endpoints.</li>
 *   <li>{@code transportista} — solo descarga y consulta.</li>
 * </ul>
 *
 * <p>Cada operación de escritura (crear, actualizar, eliminar) publica un mensaje
 * en RabbitMQ vía {@link GuiaMensajePublisher} para que el microservicio Consumer
 * persista el evento de forma asíncrona en su propia base de datos.</p>
 */
@RestController
@RequestMapping("/api/v1/guias")
@Validated
public class GuiaController {

    private final GuiaService guiaService;
    private final GuiaMensajePublisher mensajePublisher;

    public GuiaController(GuiaService guiaService, GuiaMensajePublisher mensajePublisher) {
        this.guiaService = guiaService;
        this.mensajePublisher = mensajePublisher;
    }

    // -------------------------------------------------------------------------
    // 1. POST /api/v1/guias — Crear guía (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva Guía de Despacho y publica el evento en RabbitMQ.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @PostMapping
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<GuiaResponseDTO> crearGuia(@Valid @RequestBody CrearGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.crearGuia(dto);

        // Publicar evento CREAR en RabbitMQ
        mensajePublisher.publicar(new GuiaMensajeDTO(
            response.getGuiaId(),
            response.getTransportistaId(),
            response.getFechaEnvio(),
            response.getDestinatario(),
            response.getDireccionDestino(),
            response.getPesoKg(),
            response.getDescripcionCarga(),
            response.getObservaciones(),
            GuiaMensajeDTO.Operacion.CREAR,
            Instant.now()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // 2. POST /api/v1/guias/{guiaId}/upload — Subida manual a S3 (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Sube manualmente el PDF de la guía a S3.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @PostMapping("/{guiaId}/upload")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<GuiaResponseDTO> uploadGuia(@PathVariable UUID guiaId) {
        GuiaResponseDTO response = guiaService.uploadGuia(guiaId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // 3. GET /api/v1/guias/{guiaId}/download — Descarga PDF ('admin' o 'transportista')
    // -------------------------------------------------------------------------

    /**
     * Descarga la guía vía redirect pre-firmado S3.
     * Permitido para {@code admin} y {@code transportista}.
     */
    @GetMapping("/{guiaId}/download")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin') or " +
                  "authentication.principal.claims['extension_consultaRole'].equals('transportista')")
    public void downloadGuia(@PathVariable UUID guiaId,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : "";
        guiaService.downloadGuia(guiaId, token, response);
    }

    // -------------------------------------------------------------------------
    // 4. PUT /api/v1/guias/{guiaId} — Actualizar guía (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Actualiza los campos permitidos de la guía y publica el evento en RabbitMQ.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @PutMapping("/{guiaId}")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<GuiaResponseDTO> actualizarGuia(
            @PathVariable UUID guiaId,
            @RequestBody ActualizarGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.actualizarGuia(guiaId, dto);

        // Publicar evento ACTUALIZAR en RabbitMQ
        mensajePublisher.publicar(new GuiaMensajeDTO(
            response.getGuiaId(),
            response.getTransportistaId(),
            response.getFechaEnvio(),
            response.getDestinatario(),
            response.getDireccionDestino(),
            response.getPesoKg(),
            response.getDescripcionCarga(),
            response.getObservaciones(),
            GuiaMensajeDTO.Operacion.ACTUALIZAR,
            Instant.now()
        ));

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // 5. DELETE /api/v1/guias/{guiaId} — Eliminación lógica (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Elimina lógicamente la guía y publica el evento en RabbitMQ.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @DeleteMapping("/{guiaId}")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<String> eliminarGuia(@PathVariable UUID guiaId,
                                               HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : "";
        guiaService.eliminarGuia(guiaId, token);

        // Publicar evento ELIMINAR en RabbitMQ (solo con guiaId, sin datos adicionales)
        mensajePublisher.publicar(new GuiaMensajeDTO(
            guiaId,
            null, null, null, null, null, null, null,
            GuiaMensajeDTO.Operacion.ELIMINAR,
            Instant.now()
        ));

        return ResponseEntity.ok("Guía eliminada correctamente");
    }

    // -------------------------------------------------------------------------
    // 6. GET /api/v1/guias — Consulta paginada ('admin' o 'transportista')
    // -------------------------------------------------------------------------

    /**
     * Consulta paginada de guías por transportista y mes.
     * Permitido para {@code admin} y {@code transportista}.
     */
    @GetMapping
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin') or " +
                  "authentication.principal.claims['extension_consultaRole'].equals('transportista')")
    public ResponseEntity<PaginatedResponseDTO<GuiaListItemDTO>> consultarGuias(
            @RequestParam String transportistaId,
            @RequestParam String fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponseDTO<GuiaListItemDTO> response =
                guiaService.consultarGuias(transportistaId, fecha, page, size);
        return ResponseEntity.ok(response);
    }
}
