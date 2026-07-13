package com.transportista.producer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de solicitud para publicar un mensaje de Guía de Despacho en RabbitMQ.
 *
 * <p>Recibido por {@code POST /api/v1/producer/publicar}. El producer
 * convierte este DTO en un {@link GuiaMensajeDTO} y lo envía a
 * {@code guias.queue}.</p>
 */
public class PublicarMensajeRequestDTO {

    /** UUID de la guía (puede ser null para creaciones, se genera en el servicio). */
    private UUID guiaId;

    @NotBlank(message = "El campo transportistaId es obligatorio")
    @Pattern(regexp = "[a-zA-Z0-9]+", message = "transportistaId solo acepta caracteres alfanuméricos")
    @Size(max = 50, message = "transportistaId no puede superar los 50 caracteres")
    private String transportistaId;

    @NotNull(message = "El campo fechaEnvio es obligatorio")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEnvio;

    @NotBlank(message = "El campo destinatario es obligatorio")
    private String destinatario;

    @NotBlank(message = "El campo direccionDestino es obligatorio")
    private String direccionDestino;

    @DecimalMin(value = "0.0", message = "pesoKg no puede ser negativo")
    private BigDecimal pesoKg;

    private String descripcionCarga;

    private String observaciones;

    /**
     * Operación: CREAR, ACTUALIZAR o ELIMINAR.
     * Por defecto CREAR si no se especifica.
     */
    @NotNull(message = "El campo operacion es obligatorio (CREAR | ACTUALIZAR | ELIMINAR)")
    private GuiaMensajeDTO.Operacion operacion;

    // --- Constructor por defecto ---
    public PublicarMensajeRequestDTO() {
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

    public GuiaMensajeDTO.Operacion getOperacion() { return operacion; }
    public void setOperacion(GuiaMensajeDTO.Operacion operacion) { this.operacion = operacion; }
}
