package com.transportista.guias.dto;

import com.transportista.guias.model.EstadoGuia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO compacto de un elemento de lista para consultas paginadas de Guías de Despacho.
 *
 * <p>Se utiliza como tipo del arreglo {@code content} en las respuestas del endpoint
 * {@code GET /api/v1/guias}, exponiendo solo los campos necesarios para listar guías
 * sin exponer todos los detalles del documento, según el requisito 8.2.</p>
 *
 * <p>Incluye los campos: {@code guiaId}, {@code transportistaId}, {@code fechaEnvio},
 * {@code estado}, {@code urlS3} y {@code fechaCreacion}.</p>
 *
 * <p><b>Requisitos relacionados:</b> 8.1, 8.2</p>
 */
public class GuiaListItemDTO {

    /**
     * Identificador único de la guía (UUID v4).
     */
    private UUID guiaId;

    /**
     * Identificador del transportista responsable del envío.
     */
    private String transportistaId;

    /**
     * Fecha de envío de la guía.
     */
    private LocalDate fechaEnvio;

    /**
     * Estado actual del ciclo de vida de la guía.
     *
     * @see EstadoGuia
     */
    private EstadoGuia estado;

    /**
     * URL del objeto en Amazon S3. Puede ser {@code null} si la guía aún no está subida.
     */
    private String urlS3;

    /**
     * Fecha y hora de creación de la guía en formato ISO 8601 UTC.
     */
    private Instant fechaCreacion;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public GuiaListItemDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param guiaId          identificador UUID de la guía
     * @param transportistaId identificador del transportista
     * @param fechaEnvio      fecha de envío
     * @param estado          estado del ciclo de vida
     * @param urlS3           URL de S3 (puede ser {@code null})
     * @param fechaCreacion   fecha y hora de creación en UTC
     */
    public GuiaListItemDTO(UUID guiaId, String transportistaId, LocalDate fechaEnvio,
                            EstadoGuia estado, String urlS3, Instant fechaCreacion) {
        this.guiaId = guiaId;
        this.transportistaId = transportistaId;
        this.fechaEnvio = fechaEnvio;
        this.estado = estado;
        this.urlS3 = urlS3;
        this.fechaCreacion = fechaCreacion;
    }

    // --- Getters y Setters ---

    /**
     * Devuelve el identificador único de la guía.
     *
     * @return guiaId en formato UUID v4
     */
    public UUID getGuiaId() {
        return guiaId;
    }

    /**
     * Establece el identificador único de la guía.
     *
     * @param guiaId UUID v4 de la guía
     */
    public void setGuiaId(UUID guiaId) {
        this.guiaId = guiaId;
    }

    /**
     * Devuelve el identificador del transportista.
     *
     * @return transportistaId
     */
    public String getTransportistaId() {
        return transportistaId;
    }

    /**
     * Establece el identificador del transportista.
     *
     * @param transportistaId identificador alfanumérico del transportista
     */
    public void setTransportistaId(String transportistaId) {
        this.transportistaId = transportistaId;
    }

    /**
     * Devuelve la fecha de envío.
     *
     * @return fechaEnvio
     */
    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    /**
     * Establece la fecha de envío.
     *
     * @param fechaEnvio fecha en formato {@code LocalDate}
     */
    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    /**
     * Devuelve el estado actual de la guía.
     *
     * @return estado del ciclo de vida
     * @see EstadoGuia
     */
    public EstadoGuia getEstado() {
        return estado;
    }

    /**
     * Establece el estado de la guía.
     *
     * @param estado estado del ciclo de vida
     */
    public void setEstado(EstadoGuia estado) {
        this.estado = estado;
    }

    /**
     * Devuelve la URL del objeto en S3.
     *
     * @return urlS3 o {@code null} si la guía aún no está subida
     */
    public String getUrlS3() {
        return urlS3;
    }

    /**
     * Establece la URL del objeto en S3.
     *
     * @param urlS3 URL completa del objeto almacenado en Amazon S3
     */
    public void setUrlS3(String urlS3) {
        this.urlS3 = urlS3;
    }

    /**
     * Devuelve la fecha y hora de creación en UTC.
     *
     * @return fechaCreacion como {@code Instant}
     */
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Establece la fecha y hora de creación.
     *
     * @param fechaCreacion instante de creación en UTC
     */
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "GuiaListItemDTO{" +
                "guiaId=" + guiaId +
                ", transportistaId='" + transportistaId + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", estado=" + estado +
                ", urlS3='" + urlS3 + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}
