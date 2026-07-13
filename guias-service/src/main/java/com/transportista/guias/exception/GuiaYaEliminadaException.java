package com.transportista.guias.exception;

/**
 * Excepción lanzada cuando se intenta operar sobre una guía de despacho que ya fue
 * eliminada (soft delete con estado {@code ELIMINADA}).
 *
 * <p>Corresponde a una respuesta HTTP 409 Conflict manejada por el
 * {@code GlobalExceptionHandler}.</p>
 *
 * <p><b>Requisito:</b> 7.5 — La guía ya se encuentra en estado {@code ELIMINADA}.</p>
 */
public class GuiaYaEliminadaException extends RuntimeException {

    /**
     * Crea una nueva excepción con el mensaje indicado.
     *
     * @param message descripción del error, p. ej. "La guía ya ha sido eliminada"
     */
    public GuiaYaEliminadaException(String message) {
        super(message);
    }

    /**
     * Crea una nueva excepción con mensaje y causa raíz.
     *
     * @param message descripción del error
     * @param cause   excepción original que originó este error
     */
    public GuiaYaEliminadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
