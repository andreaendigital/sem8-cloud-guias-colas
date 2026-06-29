package com.transportista.guias.exception;

import com.transportista.guias.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para la API REST de Guías de Despacho.
 *
 * <p>Centraliza el manejo de errores de la capa de controlador mediante
 * {@code @RestControllerAdvice}, devolviendo siempre una respuesta uniforme
 * con estructura {@link ErrorResponseDTO} (campos {@code status}, {@code message},
 * {@code errors} y {@code timestamp}).</p>
 *
 * <p>Mapa de excepciones manejadas:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → HTTP 400 con lista de errores por campo</li>
 *   <li>{@link HttpMessageNotReadableException} → HTTP 400 (campos no permitidos / JSON inválido)</li>
 *   <li>{@link GuiaNotFoundException} → HTTP 404</li>
 *   <li>{@link GuiaYaEliminadaException} → HTTP 409</li>
 *   <li>{@link GuiaNoDisponibleException} → HTTP 409</li>
 *   <li>{@link S3UploadException} → HTTP 502</li>
 *   <li>{@link Exception} (genérica) → HTTP 500</li>
 * </ul>
 *
 * <p><b>Requisitos relacionados:</b> 3.2, 2.4, 5.7</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // HTTP 400 — Validación de campos (@Valid / @Validated)
    // -------------------------------------------------------------------------

    /**
     * Maneja errores de validación de bean ({@code @Valid}) en los cuerpos de
     * las peticiones. Recopila todos los mensajes de error por campo y los
     * devuelve en la lista {@code errors} del {@link ErrorResponseDTO}.
     *
     * <p>Ejemplo de respuesta:</p>
     * <pre>
     * HTTP 400
     * {
     *   "status": 400,
     *   "message": "Errores de validación",
     *   "errors": ["El campo transportistaId es obligatorio",
     *              "El campo fechaEnvio es obligatorio"],
     *   "timestamp": "2025-07-15T10:30:00Z"
     * }
     * </pre>
     *
     * @param ex excepción lanzada por Spring cuando la validación de
     *           {@code @RequestBody} falla
     * @return {@link ResponseEntity} con HTTP 400 y lista de mensajes de campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex) {

        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Errores de validación",
                fieldErrors,
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 400 — JSON ilegible / campos no permitidos
    // -------------------------------------------------------------------------

    /**
     * Maneja solicitudes cuyo cuerpo JSON no puede ser deserializado, incluyendo
     * los rechazos generados por {@code @JsonIgnoreProperties(ignoreUnknown = false)}
     * cuando el payload contiene campos no permitidos.
     *
     * @param ex excepción lanzada por Spring cuando el cuerpo HTTP no puede
     *           ser leído o mapeado al tipo esperado
     * @return {@link ResponseEntity} con HTTP 400 y mensaje descriptivo
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "El cuerpo de la solicitud contiene campos no permitidos o tiene un formato inválido",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 400 — IllegalArgumentException (parámetros de consulta inválidos)
    // -------------------------------------------------------------------------

    /**
     * Maneja errores de argumentos inválidos lanzados por la capa de servicio:
     * parámetros de paginación fuera de rango, formato de fecha incorrecto,
     * o parámetros obligatorios ausentes (transportistaId, fecha).
     *
     * @param ex excepción lanzada con el mensaje descriptivo del requisito incumplido
     * @return {@link ResponseEntity} con HTTP 400 y el mensaje de la excepción
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 403 — AccessDeniedException (@PreAuthorize / permisos insuficientes)
    // -------------------------------------------------------------------------

    /**
     * Captura denegaciones de acceso lanzadas por {@code @PreAuthorize} cuando
     * el claim {@code extension_consultaRole} no tiene el rol requerido.
     * Devuelve exactamente {@code {"error": "no tiene permiso"}} con HTTP 403.
     *
     * @param ex excepción de acceso denegado
     * @return HTTP 403 con mensaje "no tiene permiso"
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(
            AccessDeniedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "no tiene permiso");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // -------------------------------------------------------------------------
    // HTTP 404 — Guía no encontrada
    // -------------------------------------------------------------------------

    /**
     * Maneja el caso en que la guía identificada por {@code guiaId} no existe
     * en la base de datos.
     *
     * @param ex excepción lanzada por el servicio cuando la guía no fue
     *           encontrada
     * @return {@link ResponseEntity} con HTTP 404 y el mensaje de la excepción
     */
    @ExceptionHandler(GuiaNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleGuiaNotFound(
            GuiaNotFoundException ex) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 409 — Conflicto de estado de guía
    // -------------------------------------------------------------------------

    /**
     * Maneja conflictos de estado de la guía: guía ya eliminada
     * ({@link GuiaYaEliminadaException}) o guía no disponible para la operación
     * solicitada ({@link GuiaNoDisponibleException}).
     *
     * <p>Ambas situaciones corresponden a un conflicto de negocio (HTTP 409
     * Conflict) dado que la guía existe pero su estado impide la operación.</p>
     *
     * @param ex excepción de conflicto de estado lanzada por el servicio
     * @return {@link ResponseEntity} con HTTP 409 y el mensaje de la excepción
     */
    @ExceptionHandler({GuiaYaEliminadaException.class, GuiaNoDisponibleException.class})
    public ResponseEntity<ErrorResponseDTO> handleGuiaConflict(
            RuntimeException ex) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 502 — Error de subida a S3
    // -------------------------------------------------------------------------

    /**
     * Maneja los fallos de subida al almacenamiento S3 tras agotar los reintentos
     * configurados con {@code @Retryable}.
     *
     * @param ex excepción lanzada por {@code S3StorageService} cuando la subida
     *           falla de forma irrecuperable
     * @return {@link ResponseEntity} con HTTP 502 y el mensaje de la excepción
     */
    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<ErrorResponseDTO> handleS3UploadException(
            S3UploadException ex) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    // -------------------------------------------------------------------------
    // HTTP 500 — Error genérico no controlado
    // -------------------------------------------------------------------------

    /**
     * Captura cualquier excepción no manejada por los handlers anteriores y
     * devuelve un error genérico HTTP 500. Registra el stack trace completo en
     * el log de nivel {@code ERROR} para facilitar el diagnóstico.
     *
     * @param ex excepción inesperada no tipificada
     * @return {@link ResponseEntity} con HTTP 500 y un mensaje genérico de error
     *         interno (sin exponer detalles internos al cliente)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {

        log.error("Error interno no controlado: {}", ex.getMessage(), ex);

        ErrorResponseDTO body = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ha ocurrido un error interno en el servidor",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
