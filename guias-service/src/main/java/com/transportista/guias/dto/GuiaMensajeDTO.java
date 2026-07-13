package com.transportista.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de mensaje que viaja en RabbitMQ entre guias-service y el Consumer.
 *
 * <p>Cuando el {@code GuiaController} crea, actualiza o elimina una guía,
 * este mensaje se publica en {@code guias.queue}. El Consumer lo deserializa
 * y persiste el evento en la tabla {@code GUIAS_EVENTOS}.</p>
 */
public class GuiaMensajeDTO implements Serializable {

    /** Tipo de operación que originó el mensaje. */
    public enum Operacion {
        CREAR, ACTUALIZAR, ELIMINAR
    }

    private UUID guiaId;
    private String transportistaId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEnvio;

    private String destinatario;
    private String direccionDestino;
    private BigDecimal pesoKg;
    private String descripcionCarga;
    private String observaciones;
    private Operacion operacion;
    private Instant fechaMensaje;

    // --- Constructor por defecto (Jackson) ---
    public GuiaMensajeDTO() {}

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
    public void setTransportistaId(String t) { this.transportistaId = t; }

    public LocalDate getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDate f) { this.fechaEnvio = f; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String d) { this.destinatario = d; }

    public String getDireccionDestino() { return direccionDestino; }
    public void setDireccionDestino(String d) { this.direccionDestino = d; }

    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal p) { this.pesoKg = p; }

    public String getDescripcionCarga() { return descripcionCarga; }
    public void setDescripcionCarga(String d) { this.descripcionCarga = d; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o; }

    public Operacion getOperacion() { return operacion; }
    public void setOperacion(Operacion op) { this.operacion = op; }

    public Instant getFechaMensaje() { return fechaMensaje; }
    public void setFechaMensaje(Instant f) { this.fechaMensaje = f; }

    @Override
    public String toString() {
        return "GuiaMensajeDTO{guiaId=" + guiaId + ", operacion=" + operacion + '}';
    }
}
