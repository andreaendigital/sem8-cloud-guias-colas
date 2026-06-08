package com.transportista.guias.model;

/**
 * Representa el estado del ciclo de vida de una Guía de Despacho.
 *
 * <ul>
 *   <li>{@link #BORRADOR} – Guía recién creada; el PDF aún no ha sido generado,
 *       o la guía fue modificada y el PDF debe regenerarse.</li>
 *   <li>{@link #GENERADA} – PDF generado y escrito en EFS, pendiente de subir a S3.</li>
 *   <li>{@link #SUBIDA} – PDF subido exitosamente a Amazon S3.</li>
 *   <li>{@link #ERROR_SUBIDA} – La subida a S3 falló tras agotar los reintentos.</li>
 *   <li>{@link #ELIMINADA} – Eliminación lógica (soft delete) aplicada a la guía.</li>
 * </ul>
 *
 * <p>Relacionado con los requisitos 3.1 (creación en estado BORRADOR) y 7.2
 * (eliminación lógica conservando el registro con estado ELIMINADA).</p>
 */
public enum EstadoGuia {

    /** Guía creada; PDF aún no generado o en proceso de regeneración. */
    BORRADOR,

    /** PDF generado y almacenado en EFS, a la espera de ser subido a S3. */
    GENERADA,

    /** PDF subido exitosamente a Amazon S3. */
    SUBIDA,

    /** La subida a S3 falló tras los reintentos configurados. */
    ERROR_SUBIDA,

    /** La guía fue eliminada de forma lógica (soft delete). */
    ELIMINADA
}
