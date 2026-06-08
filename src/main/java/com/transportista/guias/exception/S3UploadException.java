package com.transportista.guias.exception;

/**
 * Excepción lanzada cuando la subida de un archivo al bucket de Amazon S3 falla,
 * ya sea tras agotar los reintentos configurados o por un error no reintentable.
 *
 * <p>Corresponde a una respuesta HTTP 502 Bad Gateway manejada por el
 * {@code GlobalExceptionHandler}.</p>
 *
 * <p><b>Requisito:</b> 7.5 / 2.4 — Error de subida a S3 tras reintentos agotados o
 * error no reintentable (p. ej. bucket inexistente).</p>
 */
public class S3UploadException extends RuntimeException {

    /**
     * Crea una nueva excepción con el mensaje indicado.
     *
     * @param message descripción del error, p. ej. detalle del fallo de S3
     */
    public S3UploadException(String message) {
        super(message);
    }

    /**
     * Crea una nueva excepción con mensaje y causa raíz.
     *
     * @param message descripción del error
     * @param cause   excepción original del SDK de AWS u otro proveedor
     */
    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
