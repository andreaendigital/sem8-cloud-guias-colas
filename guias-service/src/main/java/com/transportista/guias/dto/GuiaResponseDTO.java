package com.transportista.guias.dto;

import com.transportista.guias.model.EstadoGuia;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de respuesta completa para una Guía de Despacho.
 *
 * <p>Se utiliza como cuerpo de respuesta en los endpoints de creación
 * ({@code POST /api/v1/guias}), actualización ({@code PUT /api/v1/guias/{guiaId}})
 * y subida ({@code POST /api/v1/guias/{guiaId}/upload}), devolviendo la
 * representación completa del estado actual de la guía.</p>
 *
 * <p>El campo {@code urlS3} puede ser {@code null} mientras la guía no haya
 * sido subida exitosamente a Amazon S3 (estado distinto a {@code SUBIDA}).</p>
 *
 * <p><b>Requisitos relacionados:</b> 3.1, 3.6, 8.2</p>
 */
public class GuiaResponseDTO {

    /**
     * Identificador único de la guía (UUID v4).
     */
    private UUID guiaId;

    /**
     * Identificador del transportista responsable del envío.
     */
    private String transportistaId;

    /**
     * Fecha de envío de la guía en formato {@code yyyy-MM-dd}.
     */
    private LocalDate fechaEnvio;

    /**
     * Nombre completo del destinatario del envío.
     */
    private String destinatario;

    /**
     * Dirección de destino del envío.
     */
    private String direccionDestino;

    /**
     * Peso de la carga en kilogramos. Puede ser {@code null}.
     */
    private BigDecimal pesoKg;

    /**
     * Descripción del contenido de la carga. Puede ser {@code null}.
     */
    private String descripcionCarga;

    /**
     * Observaciones adicionales sobre el envío. Puede ser {@code null}.
     */
    private String observaciones;

    /**
     * Estado actual del ciclo de vida de la guía.
     *
     * @see EstadoGuia
     */
    private EstadoGuia estado;

    /**
     * URL del objeto en Amazon S3. Es {@code null} hasta que el estado sea {@code SUBIDA}.
     */
    private String urlS3;

    /**
     * Fecha y hora de creación de la guía en formato ISO 8601 UTC.
     */
    private Instant fechaCreacion;

    /**
     * Fecha y hora de la última actualización en formato ISO 8601 UTC.
     * Puede ser {@code null} si la guía nunca fue modificada.
     */
    private Instant fechaActualizacion;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public GuiaResponseDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param guiaId              identificador UUID de la guía
     * @param transportistaId     identificador del transportista
     * @param fechaEnvio          fecha de envío
     * @param destinatario        nombre del destinatario
     * @param direccionDestino    dirección de destino
     * @param pesoKg              peso en kg (puede ser {@code null})
     * @param descripcionCarga    descripción de la carga (puede ser {@code null})
     * @param observaciones       observaciones (puede ser {@code null})
     * @param estado              estado del ciclo de vida
     * @param urlS3               URL de S3 (puede ser {@code null})
     * @param fechaCreacion       fecha y hora de creación en UTC
     * @param fechaActualizacion  fecha y hora de última actualización en UTC (puede ser {@code null})
     */
    public GuiaResponseDTO(UUID guiaId, String transportistaId, LocalDate fechaEnvio,
                            String destinatario, String direccionDestino, BigDecimal pesoKg,
                            String descripcionCarga, String observaciones, EstadoGuia estado,
                            String urlS3, Instant fechaCreacion, Instant fechaActualizacion) {
        this.guiaId = guiaId;
        this.transportistaId = transportistaId;
        this.fechaEnvio = fechaEnvio;
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
        this.descripcionCarga = descripcionCarga;
        this.observaciones = observaciones;
        this.estado = estado;
        this.urlS3 = urlS3;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
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
     * Devuelve el nombre del destinatario.
     *
     * @return destinatario
     */
    public String getDestinatario() {
        return destinatario;
    }

    /**
     * Establece el nombre del destinatario.
     *
     * @param destinatario nombre completo del destinatario
     */
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    /**
     * Devuelve la dirección de destino.
     *
     * @return direccionDestino
     */
    public String getDireccionDestino() {
        return direccionDestino;
    }

    /**
     * Establece la dirección de destino.
     *
     * @param direccionDestino dirección completa de destino del envío
     */
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    /**
     * Devuelve el peso en kilogramos.
     *
     * @return pesoKg o {@code null}
     */
    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    /**
     * Establece el peso en kilogramos.
     *
     * @param pesoKg peso de la carga
     */
    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    /**
     * Devuelve la descripción de la carga.
     *
     * @return descripcionCarga o {@code null}
     */
    public String getDescripcionCarga() {
        return descripcionCarga;
    }

    /**
     * Establece la descripción de la carga.
     *
     * @param descripcionCarga texto descriptivo del contenido
     */
    public void setDescripcionCarga(String descripcionCarga) {
        this.descripcionCarga = descripcionCarga;
    }

    /**
     * Devuelve las observaciones del envío.
     *
     * @return observaciones o {@code null}
     */
    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Establece las observaciones del envío.
     *
     * @param observaciones texto libre con indicaciones adicionales
     */
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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
     * @param estado nuevo estado del ciclo de vida
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

    /**
     * Devuelve la fecha y hora de la última actualización en UTC.
     *
     * @return fechaActualizacion o {@code null} si no fue modificada
     */
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    /**
     * Establece la fecha y hora de la última actualización.
     *
     * @param fechaActualizacion instante de la última modificación en UTC
     */
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public String toString() {
        return "GuiaResponseDTO{" +
                "guiaId=" + guiaId +
                ", transportistaId='" + transportistaId + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", destinatario='" + destinatario + '\'' +
                ", direccionDestino='" + direccionDestino + '\'' +
                ", pesoKg=" + pesoKg +
                ", descripcionCarga='" + descripcionCarga + '\'' +
                ", observaciones='" + observaciones + '\'' +
                ", estado=" + estado +
                ", urlS3='" + urlS3 + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaActualizacion=" + fechaActualizacion +
                '}';
    }
}
