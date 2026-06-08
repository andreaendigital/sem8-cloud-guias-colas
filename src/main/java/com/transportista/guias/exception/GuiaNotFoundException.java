package com.transportista.guias.exception;

/**
 * Excepción lanzada cuando una guía de despacho no es encontrada en la base de datos.
 *
 * <p>Corresponde a una respuesta HTTP 404 Not Found manejada por el
 * {@code GlobalExceptionHandler}.</p>
 *
 * <p><b>Requisito:</b> 4.3 — La guía identificada por {@code guiaId} no existe en la base
 * de datos.</p>
 */
public class GuiaNotFoundException extends RuntimeException {

    /**
     * Crea una nueva excepción con el mensaje indicado.
     *
     * @param message descripción del error, p. ej. "Guía no encontrada"
     */
    public GuiaNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea una nueva excepción con mensaje y causa raíz.
     *
     * @param message descripción del error
     * @param cause   excepción original que originó este error
     */
    public GuiaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
