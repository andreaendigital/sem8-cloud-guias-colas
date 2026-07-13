package com.transportista.producer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de mensaje que viaja en RabbitMQ entre el Producer y el Consumer.
 *
 * <p>Encapsula todos los datos de una Guía de Despacho necesarios para que
 * el Consumer pueda persistir el evento en la base de datos de eventos.
 * Se serializa/deserializa como JSON usando Jackson.</p>
 *
 * <p>El campo {@code operacion} indica la operación que origina el mensaje:
 * {@code CREAR}, {@code ACTUALIZAR} o {@code ELIMINAR}.</p>
 */
public class GuiaMensajeDTO implements Serializable {

    /** Tipo de operación que originó el mensaje. */
    public enum Operacion {
        CREAR, ACTUALIZAR, ELIMINAR
    }

    /** Identificador único de la guía (UUID v4). */
    private UUID guiaId;

    /** Identificador del transportista responsable del envío. */
    private String transportistaId;

    /** Fecha de envío en formato yyyy-MM-dd. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEnvio;

    /** Nombre completo del destinatario. */
    private String destinatario;

    /** Dirección de destino del envío. */
    private String direccionDestino;

    /** Peso de la carga en kilogramos (puede ser null). */
    private BigDecimal pesoKg;

    /** Descripción del contenido de la carga (puede ser null). */
    private String descripcionCarga;

    /** Observaciones adicionales (puede ser null). */
    private String observaciones;

    /** Operación que originó el mensaje: CREAR, ACTUALIZAR o ELIMINAR. */
    private Operacion operacion;

    /** Momento en que se generó el mensaje (UTC). */
    private Instant fechaMensaje;

    // --- Constructor por defecto (requerido por Jackson) ---
    public GuiaMensajeDTO() {
    }

    // --- Constructor completo ---
    public GuiaMensajeDTO(UUID guiaId, String transportistaId, LocalDate fechaEnvio,
                           String destinatario, String direccionDestino, BigDecimal pesoKg,
                           String descripcionCarga, String observaciones,
                           Operacion operacion, Instant fechaMensaje) {
        this.guiaId = guiaId;
        this.transportistaId = transportistaId;
        this.fechaEnvio = fechaEnvio;
        this.destinatario = destinatario;
        this.direccionDestino = direccionDestino;
        this.pesoKg = pesoKg;
        this.descripcionCarga = descripcionCarga;
        this.observaciones = observaciones;
        this.operacion = operacion;
        this.fechaMensaje = fechaMensaje;
    }

    // --- Getters y Setters ---

    public UUID getGuiaId() { return guiaId; }
    public void setGuiaId(UUID guiaId) { this.guiaId = guiaId; }

    public String getTransportistaId() { return transportistaId; }
    public void setTransportistaId(String transportistaId) { this.transportistaId = transportistaId; }

    public LocalDate getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDate fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getDireccionDestino() { return direccionDestino; }
    public void setDireccionDestino(String direccionDestino) { this.direccionDestino = direccionDestino; }

    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }

    public String getDescripcionCarga() { return descripcionCarga; }
    public void setDescripcionCarga(String descripcionCarga) { this.descripcionCarga = descripcionCarga; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Operacion getOperacion() { return operacion; }
    public void setOperacion(Operacion operacion) { this.operacion = operacion; }

    public Instant getFechaMensaje() { return fechaMensaje; }
    public void setFechaMensaje(Instant fechaMensaje) { this.fechaMensaje = fechaMensaje; }

    @Override
    public String toString() {
        return "GuiaMensajeDTO{guiaId=" + guiaId +
               ", transportistaId='" + transportistaId + '\'' +
               ", operacion=" + operacion +
               ", fechaMensaje=" + fechaMensaje + '}';
    }
}
