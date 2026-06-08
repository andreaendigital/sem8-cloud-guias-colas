package com.transportista.guias.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de respuesta estándar para errores de la API.
 *
 * <p>Utilizado por el {@code GlobalExceptionHandler} para devolver una
 * estructura uniforme en todas las respuestas de error HTTP (4xx y 5xx).
 * El campo {@code errors} está disponible para detallar errores de validación
 * a nivel de campo (por ejemplo, múltiples campos inválidos en un {@code POST})
 * y puede ser {@code null} cuando el error es de naturaleza general.</p>
 *
 * <p>Ejemplo de respuesta HTTP 400 con validación de campos:</p>
 * <pre>
 * {
 *   "status": 400,
 *   "message": "Errores de validación",
 *   "errors": ["El campo transportistaId es obligatorio", "El campo fechaEnvio es obligatorio"],
 *   "timestamp": "2025-07-15T10:30:00Z"
 * }
 * </pre>
 *
 * <p><b>Requisitos relacionados:</b> 3.2, 2.4, 5.7</p>
 */
public class ErrorResponseDTO {

    /**
     * Código de estado HTTP de la respuesta (ej. 400, 404, 409, 500).
     */
    private int status;

    /**
     * Mensaje descriptivo del error.
     */
    private String message;

    /**
     * Lista de mensajes de error específicos por campo, para respuestas de
     * validación ({@code HTTP 400}). Puede ser {@code null} para errores generales.
     */
    private List<String> errors;

    /**
     * Fecha y hora en que ocurrió el error en formato ISO 8601 UTC.
     */
    private Instant timestamp;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public ErrorResponseDTO() {
    }

    /**
     * Constructor para errores generales sin lista de campos.
     *
     * @param status    código de estado HTTP
     * @param message   mensaje descriptivo del error
     * @param timestamp instante en que ocurrió el error
     */
    public ErrorResponseDTO(int status, String message, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.errors = null;
        this.timestamp = timestamp;
    }

    /**
     * Constructor completo con lista de errores de validación.
     *
     * @param status    código de estado HTTP
     * @param message   mensaje descriptivo del error
     * @param errors    lista de mensajes por campo (puede ser {@code null})
     * @param timestamp instante en que ocurrió el error
     */
    public ErrorResponseDTO(int status, String message, List<String> errors, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = timestamp;
    }

    // --- Getters y Setters ---

    /**
     * Devuelve el código de estado HTTP.
     *
     * @return status (ej. 400, 404, 500)
     */
    public int getStatus() {
        return status;
    }

    /**
     * Establece el código de estado HTTP.
     *
     * @param status código de estado HTTP
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Devuelve el mensaje descriptivo del error.
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Establece el mensaje del error.
     *
     * @param message texto descriptivo del error
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Devuelve la lista de errores de validación por campo.
     *
     * @return lista de mensajes de error o {@code null} para errores generales
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Establece la lista de errores de validación por campo.
     *
     * @param errors lista de mensajes por campo; puede ser {@code null}
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * Devuelve el instante en que ocurrió el error.
     *
     * @return timestamp en UTC
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Establece el instante del error.
     *
     * @param timestamp fecha y hora del error en UTC
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ErrorResponseDTO{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", errors=" + errors +
                ", timestamp=" + timestamp +
                '}';
    }
}
