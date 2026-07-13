package com.transportista.guias.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * DTO de solicitud para la actualización parcial de una Guía de Despacho.
 *
 * <p>Solo contiene los campos que el sistema permite modificar, conforme al
 * requisito 6.2: {@code destinatario}, {@code direccionDestino}, {@code pesoKg},
 * {@code descripcionCarga} y {@code observaciones}.</p>
 *
 * <p>La anotación {@code @JsonIgnoreProperties(ignoreUnknown = false)} hace que
 * Jackson lance una excepción ante cualquier campo desconocido en el cuerpo JSON,
 * cumpliendo así la restricción de rechazar campos no modificables (campos no
 * incluidos en este DTO, como {@code transportistaId} o {@code fechaEnvio}).</p>
 *
 * <p>Todos los campos son opcionales; el servicio actualiza únicamente los campos
 * cuyo valor sea distinto de {@code null} en la solicitud.</p>
 *
 * <p><b>Requisitos relacionados:</b> 6.2</p>
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ActualizarGuiaRequestDTO {

    /**
     * Nuevo nombre del destinatario del envío.
     * Opcional; si es {@code null}, el campo no se modifica.
     */
    private String destinatario;

    /**
     * Nueva dirección de destino del envío.
     * Opcional; si es {@code null}, el campo no se modifica.
     */
    private String direccionDestino;

    /**
     * Nuevo peso de la carga en kilogramos.
     * Opcional; si es {@code null}, el campo no se modifica.
     */
    private BigDecimal pesoKg;

    /**
     * Nueva descripción del contenido de la carga.
     * Opcional; si es {@code null}, el campo no se modifica.
     */
    private String descripcionCarga;

    /**
     * Nuevas observaciones adicionales sobre el envío.
     * Opcional; si es {@code null}, el campo no se modifica.
     */
    private String observaciones;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public ActualizarGuiaRequestDTO() {
    }

    /**
     * Constructor con todos los campos modificables.
     *
     * @param destinatario     nuevo nombre del destinatario (puede ser {@code null})
     * @param direccionDestino nueva dirección de destino (puede ser {@code null})
     * @param pesoKg           nuevo peso en kg (puede ser {@code null})
     * @param descripcionCarga nueva descripción de la carga (puede ser {@code null})
     * @param observaciones    nuevas observaciones (puede ser {@code null})
     */
    public ActualizarGuiaRequestDTO(String destinatario, String direccionDestino,
                                     BigDecimal pesoKg, String descripcionCarga,
                                     String observaciones) {
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
        this.descripcionCarga = descripcionCarga;
        this.observaciones = observaciones;
    }

    // --- Getters y Setters ---

    /**
     * Devuelve el nuevo nombre del destinatario.
     *
     * @return destinatario o {@code null} si no fue especificado
     */
    public String getDestinatario() {
        return destinatario;
    }

    /**
     * Establece el nuevo nombre del destinatario.
     *
     * @param destinatario nombre completo del destinatario
     */
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    /**
     * Devuelve la nueva dirección de destino.
     *
     * @return direccionDestino o {@code null} si no fue especificado
     */
    public String getDireccionDestino() {
        return direccionDestino;
    }

    /**
     * Establece la nueva dirección de destino.
     *
     * @param direccionDestino dirección completa del destino del envío
     */
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    /**
     * Devuelve el nuevo peso en kilogramos.
     *
     * @return pesoKg o {@code null} si no fue especificado
     */
    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    /**
     * Establece el nuevo peso en kilogramos.
     *
     * @param pesoKg peso mayor o igual a 0.0
     */
    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    /**
     * Devuelve la nueva descripción de la carga.
     *
     * @return descripcionCarga o {@code null} si no fue especificado
     */
    public String getDescripcionCarga() {
        return descripcionCarga;
    }

    /**
     * Establece la nueva descripción de la carga.
     *
     * @param descripcionCarga texto descriptivo del contenido del envío
     */
    public void setDescripcionCarga(String descripcionCarga) {
        this.descripcionCarga = descripcionCarga;
    }

    /**
     * Devuelve las nuevas observaciones del envío.
     *
     * @return observaciones o {@code null} si no fue especificado
     */
    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Establece las nuevas observaciones del envío.
     *
     * @param observaciones texto libre con indicaciones adicionales
     */
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "ActualizarGuiaRequestDTO{" +
                "destinatario='" + destinatario + '\'' +
                ", direccionDestino='" + direccionDestino + '\'' +
                ", pesoKg=" + pesoKg +
                ", descripcionCarga='" + descripcionCarga + '\'' +
                ", observaciones='" + observaciones + '\'' +
                '}';
    }
}
