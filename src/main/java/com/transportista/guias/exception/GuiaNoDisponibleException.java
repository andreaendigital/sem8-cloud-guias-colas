package com.transportista.guias.exception;

/**
 * Excepción lanzada cuando una guía de despacho no está disponible para la operación
 * solicitada debido a su estado actual.
 *
 * <p>Ejemplos de uso:</p>
 * <ul>
 *   <li>Intentar descargar una guía que no ha sido subida a S3 (estado distinto a
 *       {@code SUBIDA}).</li>
 *   <li>Intentar subir una guía que ya se encuentra en estado {@code SUBIDA}.</li>
 *   <li>Intentar modificar o subir una guía en estado {@code ELIMINADA}.</li>
 * </ul>
 *
 * <p>Corresponde a una respuesta HTTP 409 Conflict manejada por el
 * {@code GlobalExceptionHandler}.</p>
 *
 * <p><b>Requisito:</b> 5.5 — La guía no está disponible para descarga; también aplica
 * a otros flujos de estado definidos en el diseño.</p>
 */
public class GuiaNoDisponibleException extends RuntimeException {

    /**
     * Crea una nueva excepción con el mensaje indicado.
     *
     * @param message descripción del error, p. ej. "La guía no está disponible para descarga"
     */
    public GuiaNoDisponibleException(String message) {
        super(message);
    }

    /**
     * Crea una nueva excepción con mensaje y causa raíz.
     *
     * @param message descripción del error
     * @param cause   excepción original que originó este error
     */
    public GuiaNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
