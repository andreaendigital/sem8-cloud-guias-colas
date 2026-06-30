package com.transportista.guias.controller;

import com.transportista.guias.dto.ActualizarGuiaRequestDTO;
import com.transportista.guias.dto.CrearGuiaRequestDTO;
import com.transportista.guias.dto.GuiaListItemDTO;
import com.transportista.guias.dto.GuiaResponseDTO;
import com.transportista.guias.dto.PaginatedResponseDTO;
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

import java.util.UUID;

/**
 * Controlador REST para la gestión del ciclo de vida de las Guías de Despacho.
 *
 * <p>Seguridad basada en OAuth2 / Azure AD B2C con claim {@code extension_consultaRole}:</p>
 * <ul>
 *   <li>{@code admin} — acceso total a todos los endpoints.</li>
 *   <li>{@code transportista} — solo descarga y consulta.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/guias")
@Validated
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    // -------------------------------------------------------------------------
    // 1. POST /api/v1/guias — Crear guía (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva Guía de Despacho.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @PostMapping
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<GuiaResponseDTO> crearGuia(@Valid @RequestBody CrearGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.crearGuia(dto);
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
     * Actualiza los campos permitidos de la guía.
     * Requiere claim {@code extension_consultaRole = 'admin'}.
     */
    @PutMapping("/{guiaId}")
    @PreAuthorize("authentication.principal.claims['extension_consultaRole'].equals('admin')")
    public ResponseEntity<GuiaResponseDTO> actualizarGuia(
            @PathVariable UUID guiaId,
            @RequestBody ActualizarGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.actualizarGuia(guiaId, dto);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // 5. DELETE /api/v1/guias/{guiaId} — Eliminación lógica (solo 'admin')
    // -------------------------------------------------------------------------

    /**
     * Elimina lógicamente la guía (soft delete).
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
