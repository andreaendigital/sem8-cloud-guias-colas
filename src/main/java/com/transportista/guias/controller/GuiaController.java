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
 * <p>Expone los siguientes endpoints bajo el prefijo {@code /api/v1/guias}:</p>
 * <ul>
 *   <li>{@code POST /api/v1/guias} — Crear guía (HTTP 201)</li>
 *   <li>{@code POST /api/v1/guias/{guiaId}/upload} — Subida manual a S3 (HTTP 200)</li>
 *   <li>{@code GET  /api/v1/guias/{guiaId}/download} — Descarga vía redirect (HTTP 302)</li>
 *   <li>{@code PUT  /api/v1/guias/{guiaId}} — Actualizar datos (HTTP 200)</li>
 *   <li>{@code DELETE /api/v1/guias/{guiaId}} — Eliminación lógica (HTTP 200)</li>
 *   <li>{@code GET  /api/v1/guias} — Consulta paginada (HTTP 200)</li>
 * </ul>
 *
 * <p>Toda la lógica de negocio se delega en {@link GuiaService}. La validación de
 * entrada se realiza con {@code @Valid} y Bean Validation; los errores se centralizan
 * en {@code GlobalExceptionHandler}.</p>
 *
 * <p><b>Requisitos cubiertos:</b> 3.1, 4.1, 5.2, 6.1, 7.1, 8.1</p>
 */
@RestController
@RequestMapping("/api/v1/guias")
@Validated
public class GuiaController {

    private final GuiaService guiaService;

    /**
     * Constructor con inyección del servicio principal de negocio.
     *
     * @param guiaService servicio que implementa el ciclo de vida de las guías;
     *                    no debe ser {@code null}.
     */
    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/guias — Crear guía
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva Guía de Despacho a partir del cuerpo JSON de la solicitud.
     *
     * <p>La guía se persiste con estado {@code BORRADOR} y, de forma asíncrona y no
     * bloqueante, se dispara la generación del PDF y su subida a S3. La respuesta
     * incluye el {@code guiaId} generado.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>201 Created — guía creada exitosamente.</li>
     *   <li>400 Bad Request — campos obligatorios ausentes o formato inválido.</li>
     *   <li>500 Internal Server Error — fallo al persistir en base de datos.</li>
     * </ul>
     *
     * @param dto  datos de la guía a crear; validado con {@code @Valid}.
     * @return {@link ResponseEntity} con HTTP 201 y el {@link GuiaResponseDTO} creado.
     */
    @PostMapping
    public ResponseEntity<GuiaResponseDTO> crearGuia(@Valid @RequestBody CrearGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.crearGuia(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/guias/{guiaId}/upload — Subida manual a S3
    // -------------------------------------------------------------------------

    /**
     * Dispara manualmente la subida del PDF de la guía identificada por {@code guiaId}
     * a Amazon S3.
     *
     * <p>Útil cuando la subida automática (asíncrona) falló y la guía quedó en
     * estado {@code ERROR_SUBIDA} o {@code GENERADA}. El PDF debe existir en EFS.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>200 OK — PDF subido exitosamente, retorna la guía actualizada.</li>
     *   <li>404 Not Found — guía o PDF no encontrados.</li>
     *   <li>409 Conflict — guía ya subida o eliminada.</li>
     *   <li>502 Bad Gateway — error de comunicación con S3.</li>
     * </ul>
     *
     * @param guiaId identificador UUID de la guía a subir.
     * @return {@link ResponseEntity} con HTTP 200 y el {@link GuiaResponseDTO} actualizado.
     */
    @PostMapping("/{guiaId}/upload")
    public ResponseEntity<GuiaResponseDTO> uploadGuia(@PathVariable UUID guiaId) {
        GuiaResponseDTO response = guiaService.uploadGuia(guiaId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/guias/{guiaId}/download — Descarga con redirect
    // -------------------------------------------------------------------------

    /**
     * Redirige al cliente a la URL pre-firmada de S3 para descargar la guía.
     *
     * <p>Valida el token JWT del encabezado {@code Authorization} (ya validado por
     * {@code JwtAuthFilter}), verifica que el usuario tenga permiso sobre el
     * {@code transportistaId} de la guía y genera un redirect HTTP 302 a la URL
     * pre-firmada con vigencia de 15 minutos.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>302 Found — redirect a la URL pre-firmada de S3.</li>
     *   <li>401 Unauthorized — token ausente, expirado o firma inválida.</li>
     *   <li>403 Forbidden — sin permiso para descargar la guía del transportista.</li>
     *   <li>404 Not Found — guía no encontrada o eliminada.</li>
     *   <li>409 Conflict — guía no disponible (no está en estado SUBIDA).</li>
     *   <li>502 Bad Gateway — error al generar la URL pre-firmada.</li>
     * </ul>
     *
     * @param guiaId   identificador UUID de la guía a descargar.
     * @param request  solicitud HTTP usada para extraer el token del header
     *                 {@code Authorization}.
     * @param response respuesta HTTP donde se escribe el redirect.
     */
    @GetMapping("/{guiaId}/download")
    public void downloadGuia(@PathVariable UUID guiaId,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        // Strip "Bearer " prefix — JwtAuthFilter already validated the token
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : "";
        guiaService.downloadGuia(guiaId, token, response);
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/guias/{guiaId} — Actualizar guía
    // -------------------------------------------------------------------------

    /**
     * Actualiza los campos permitidos de la guía identificada por {@code guiaId}.
     *
     * <p>Solo se admiten los campos {@code destinatario}, {@code direccionDestino},
     * {@code pesoKg}, {@code descripcionCarga} y {@code observaciones}. Cualquier
     * otro campo en el cuerpo genera HTTP 400 gracias a
     * {@code @JsonIgnoreProperties(ignoreUnknown=false)} del DTO.</p>
     *
     * <p>Si la guía estaba en estado {@code SUBIDA}, regresa a {@code BORRADOR} y se
     * dispara la regeneración asíncrona del PDF.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>200 OK — guía actualizada, retorna la guía completa.</li>
     *   <li>400 Bad Request — campos no permitidos o valores inválidos.</li>
     *   <li>404 Not Found — guía no encontrada.</li>
     *   <li>409 Conflict — guía eliminada.</li>
     *   <li>500 Internal Server Error — fallo al persistir.</li>
     * </ul>
     *
     * @param guiaId identificador UUID de la guía a actualizar.
     * @param dto    campos a modificar; los campos nulos se ignoran.
     * @return {@link ResponseEntity} con HTTP 200 y el {@link GuiaResponseDTO} actualizado.
     */
    @PutMapping("/{guiaId}")
    public ResponseEntity<GuiaResponseDTO> actualizarGuia(
            @PathVariable UUID guiaId,
            @RequestBody ActualizarGuiaRequestDTO dto) {
        GuiaResponseDTO response = guiaService.actualizarGuia(guiaId, dto);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/guias/{guiaId} — Eliminación lógica
    // -------------------------------------------------------------------------

    /**
     * Realiza la eliminación lógica (soft delete) de la guía identificada por
     * {@code guiaId}.
     *
     * <p>Solo usuarios con rol {@code ROLE_ADMIN} pueden invocar este endpoint
     * (configurado en {@code JwtSecurityConfig}). La guía permanece en base de datos
     * con {@code eliminado=true} y estado {@code ELIMINADA}. El objeto en S3 también
     * se elimina de forma no crítica.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>200 OK — guía eliminada correctamente.</li>
     *   <li>403 Forbidden — sin rol ADMIN.</li>
     *   <li>404 Not Found — guía no encontrada.</li>
     *   <li>409 Conflict — guía ya eliminada.</li>
     * </ul>
     *
     * @param guiaId  identificador UUID de la guía a eliminar.
     * @param request solicitud HTTP para extraer el token JWT.
     * @return {@link ResponseEntity} con HTTP 200 y el mensaje de confirmación.
     */
    @DeleteMapping("/{guiaId}")
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
    // GET /api/v1/guias — Consulta paginada
    // -------------------------------------------------------------------------

    /**
     * Consulta las guías de un transportista filtradas por mes, con paginación.
     *
     * <p>Devuelve únicamente guías no eliminadas ({@code eliminado=false}).
     * Los parámetros {@code page} (base 0) y {@code size} (1–100) son opcionales
     * con valores por defecto 0 y 20 respectivamente.</p>
     *
     * <p><b>Respuestas:</b></p>
     * <ul>
     *   <li>200 OK — lista paginada de guías (puede ser vacía).</li>
     *   <li>400 Bad Request — parámetros obligatorios ausentes, formato de fecha
     *       incorrecto o paginación fuera de rango.</li>
     * </ul>
     *
     * @param transportistaId identificador del transportista; obligatorio.
     * @param fecha           mes de consulta en formato {@code YYYYMM}; obligatorio.
     * @param page            número de página (base 0); defecto 0.
     * @param size            tamaño de página (1–100); defecto 20.
     * @return {@link ResponseEntity} con HTTP 200 y el
     *         {@link PaginatedResponseDTO} de {@link GuiaListItemDTO}.
     */
    @GetMapping
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
