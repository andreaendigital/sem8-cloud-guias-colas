package com.transportista.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de solicitud para la creación de una nueva Guía de Despacho.
 *
 * <p>Contiene los campos necesarios para registrar un nuevo envío en el sistema.
 * Los campos {@code transportistaId}, {@code fechaEnvio}, {@code destinatario} y
 * {@code direccionDestino} son obligatorios; el resto son opcionales.</p>
 *
 * <p>Las anotaciones de Bean Validation garantizan que los datos de entrada
 * cumplan con las restricciones definidas antes de que lleguen a la capa de servicio,
 * conforme a los requisitos 3.2, 3.4 y 3.5.</p>
 */
public class CrearGuiaRequestDTO {

    /**
     * Identificador único del transportista responsable del envío.
     * Debe ser alfanumérico y no superar los 50 caracteres.
     */
    @NotBlank(message = "El campo transportistaId es obligatorio")
    @Pattern(
            regexp = "[a-zA-Z0-9]+",
            message = "El campo transportistaId solo puede contener caracteres alfanuméricos"
    )
    @Size(max = 50, message = "El campo transportistaId no puede superar los 50 caracteres")
    private String transportistaId;

    /**
     * Fecha de envío de la guía en formato {@code yyyy-MM-dd} (ISO 8601).
     * Campo obligatorio.
     */
    @NotNull(message = "El campo fechaEnvio es obligatorio")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEnvio;

    /**
     * Nombre completo del destinatario del envío.
     * Campo obligatorio y no puede estar en blanco.
     */
    @NotBlank(message = "El campo destinatario es obligatorio")
    private String destinatario;

    /**
     * Dirección de destino del envío.
     * Campo obligatorio y no puede estar en blanco.
     */
    @NotBlank(message = "El campo direccionDestino es obligatorio")
    private String direccionDestino;

    /**
     * Peso de la carga en kilogramos.
     * Opcional; si se proporciona, debe ser mayor o igual a 0.0.
     */
    @DecimalMin(value = "0.0", message = "El campo pesoKg no puede ser negativo")
    private BigDecimal pesoKg;

    /**
     * Descripción breve del contenido de la carga.
     * Campo opcional.
     */
    private String descripcionCarga;

    /**
     * Observaciones adicionales sobre el envío (ej. "Frágil", "Urgente").
     * Campo opcional.
     */
    private String observaciones;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public CrearGuiaRequestDTO() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param transportistaId  identificador del transportista
     * @param fechaEnvio       fecha de envío
     * @param destinatario     nombre del destinatario
     * @param direccionDestino dirección de destino
     * @param pesoKg           peso en kg (puede ser {@code null})
     * @param descripcionCarga descripción de la carga (puede ser {@code null})
     * @param observaciones    observaciones adicionales (puede ser {@code null})
     */
    public CrearGuiaRequestDTO(String transportistaId, LocalDate fechaEnvio, String destinatario,
                                String direccionDestino, BigDecimal pesoKg, String descripcionCarga,
                                String observaciones) {
        this.transportistaId = transportistaId;
        this.fechaEnvio = fechaEnvio;
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
        this.descripcionCarga = descripcionCarga;
        this.observaciones = observaciones;
    }

    // --- Getters y Setters ---

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
     * @param transportistaId identificador alfanumérico, máximo 50 caracteres
     */
    public void setTransportistaId(String transportistaId) {
        this.transportistaId = transportistaId;
    }

    /**
     * Devuelve la fecha de envío.
     *
     * @return fechaEnvio en formato {@code LocalDate}
     */
    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    /**
     * Establece la fecha de envío.
     *
     * @param fechaEnvio fecha en formato {@code yyyy-MM-dd}
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
     * @param direccionDestino dirección completa del destino del envío
     */
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    /**
     * Devuelve el peso en kilogramos.
     *
     * @return pesoKg o {@code null} si no fue especificado
     */
    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    /**
     * Establece el peso en kilogramos.
     *
     * @param pesoKg peso mayor o igual a 0.0
     */
    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    /**
     * Devuelve la descripción de la carga.
     *
     * @return descripcionCarga o {@code null} si no fue especificado
     */
    public String getDescripcionCarga() {
        return descripcionCarga;
    }

    /**
     * Establece la descripción de la carga.
     *
     * @param descripcionCarga texto descriptivo del contenido del envío
     */
    public void setDescripcionCarga(String descripcionCarga) {
        this.descripcionCarga = descripcionCarga;
    }

    /**
     * Devuelve las observaciones del envío.
     *
     * @return observaciones o {@code null} si no fue especificado
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

    @Override
    public String toString() {
        return "CrearGuiaRequestDTO{" +
                "transportistaId='" + transportistaId + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", destinatario='" + destinatario + '\'' +
                ", direccionDestino='" + direccionDestino + '\'' +
                ", pesoKg=" + pesoKg +
                ", descripcionCarga='" + descripcionCarga + '\'' +
                ", observaciones='" + observaciones + '\'' +
                '}';
    }
}
