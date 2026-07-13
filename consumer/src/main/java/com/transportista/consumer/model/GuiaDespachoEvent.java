package com.transportista.consumer.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA que representa un evento de Guía de Despacho recibido desde RabbitMQ.
 *
 * <p>Se persiste en la tabla {@code GUIAS_EVENTOS}, que es distinta a la tabla
 * {@code guias} del guias-service. Cada fila corresponde a un mensaje consumido
 * de la cola {@code guias.queue}.</p>
 *
 * <p>El campo {@code operacion} indica si el evento fue CREAR, ACTUALIZAR o ELIMINAR.
 * El campo {@code procesadoEn} registra cuándo fue procesado por el Consumer.</p>
 */
@Entity
@Table(name = "GUIAS_EVENTOS")
public class GuiaDespachoEvent {

    /** Clave primaria autoincremental del evento (no es el guiaId del negocio). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** UUID de la guía de negocio (puede repetirse si hay varios eventos por guía). */
    @Column(name = "guia_id", nullable = false)
    private UUID guiaId;

    /** Identificador del transportista. Puede ser null en eventos ELIMINAR. */
    @Column(name = "transportista_id", length = 50)
    private String transportistaId;

    /** Fecha de envío de la guía. Puede ser null en eventos ELIMINAR. */
    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio;

    /** Nombre del destinatario. Puede ser null en eventos ELIMINAR. */
    @Column(name = "destinatario", length = 255)
    private String destinatario;

    /** Dirección de destino. Puede ser null en eventos ELIMINAR. */
    @Column(name = "direccion_destino", length = 500)
    private String direccionDestino;

    /** Peso de la carga en kg. Opcional. */
    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    /** Descripción del contenido de la carga. Opcional. */
    @Column(name = "descripcion_carga", length = 1000)
    private String descripcionCarga;

    /** Observaciones adicionales. Opcional. */
    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    /** Tipo de operación: CREAR, ACTUALIZAR o ELIMINAR. */
    @Column(name = "operacion", nullable = false, length = 20)
    private String operacion;

    /** Timestamp original del mensaje publicado por el Producer / guias-service. */
    @Column(name = "fecha_mensaje")
    private Instant fechaMensaje;

    /** Timestamp de cuando este Consumer procesó y persistió el evento. */
    @Column(name = "procesado_en", nullable = false, updatable = false)
    private Instant procesadoEn;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onPrePersist() {
        this.procesadoEn = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructor por defecto (JPA)
    // -------------------------------------------------------------------------
    public GuiaDespachoEvent() {}

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getOperacion() { return operacion; }
    public void setOperacion(String op) { this.operacion = op; }

    public Instant getFechaMensaje() { return fechaMensaje; }
    public void setFechaMensaje(Instant f) { this.fechaMensaje = f; }

    public Instant getProcesadoEn() { return procesadoEn; }
    public void setProcesadoEn(Instant p) { this.procesadoEn = p; }

    @Override
    public String toString() {
        return "GuiaDespachoEvent{id=" + id + ", guiaId=" + guiaId +
               ", operacion='" + operacion + "', procesadoEn=" + procesadoEn + '}';
    }
}
