package com.transportista.guias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA que representa una Guía de Despacho en el sistema.
 *
 * <p>Cada guía es identificada de forma única por un {@link UUID} auto-generado
 * ({@code guiaId}) y pasa por varios estados a lo largo de su ciclo de vida:
 * {@link EstadoGuia#BORRADOR}, {@link EstadoGuia#GENERADA}, {@link EstadoGuia#SUBIDA},
 * {@link EstadoGuia#ERROR_SUBIDA} y {@link EstadoGuia#ELIMINADA}.</p>
 *
 * <p>La eliminación es lógica (soft delete): el campo {@code eliminado} se marca
 * {@code true} y el estado cambia a {@link EstadoGuia#ELIMINADA} sin borrar el
 * registro de la base de datos.</p>
 *
 * <p>Relacionado con los requisitos: 3.3 (UUID v4 único), 3.6 (fechaCreacion en UTC),
 * 6.6 (fechaActualizacion en UTC).</p>
 */
@Entity
@Table(name = "guias")
public class Guia {

    /**
     * Identificador único de la guía. Se genera automáticamente como UUID v4
     * en el momento de la persistencia y no puede actualizarse posteriormente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guia_id", updatable = false, nullable = false)
    private UUID guiaId;

    /**
     * Identificador del transportista responsable del envío.
     * Solo se permiten caracteres alfanuméricos y longitud máxima de 50 caracteres.
     */
    @Column(name = "transportista_id", nullable = false, length = 50)
    private String transportistaId;

    /**
     * Fecha en que se realiza el envío, en formato {@code YYYY-MM-DD} (ISO 8601).
     */
    @Column(name = "fecha_envio", nullable = false)
    private LocalDate fechaEnvio;

    /**
     * Nombre o razón social del destinatario del paquete o carga.
     */
    @Column(name = "destinatario", nullable = false, length = 255)
    private String destinatario;

    /**
     * Dirección completa del lugar de destino de la carga.
     */
    @Column(name = "direccion_destino", nullable = false, length = 500)
    private String direccionDestino;

    /**
     * Peso de la carga en kilogramos. Campo opcional.
     */
    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    /**
     * Descripción breve del contenido de la carga. Campo opcional.
     */
    @Column(name = "descripcion_carga", length = 1000)
    private String descripcionCarga;

    /**
     * Observaciones adicionales sobre el envío (p. ej., "Frágil"). Campo opcional.
     */
    @Column(name = "observaciones", length = 2000)
    private String observaciones;

    /**
     * Estado actual de la guía dentro de su ciclo de vida.
     * Se persiste como cadena de texto ({@link EnumType#STRING}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoGuia estado;

    /**
     * URL del objeto almacenado en Amazon S3 una vez que la guía ha sido subida.
     * Es {@code null} mientras la guía no se encuentre en estado {@link EstadoGuia#SUBIDA}.
     */
    @Column(name = "url_s3", length = 1000)
    private String urlS3;

    /**
     * Fecha y hora de creación de la guía en formato UTC (ISO 8601).
     * Se asigna automáticamente mediante {@link #onPrePersist()} y no puede modificarse.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    /**
     * Fecha y hora de la última modificación de la guía en formato UTC (ISO 8601).
     * Es {@code null} si la guía nunca ha sido modificada después de su creación.
     */
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    /**
     * Indicador de eliminación lógica. Cuando es {@code true}, la guía se considera
     * eliminada y no aparece en los resultados de consulta normales.
     */
    @Column(name = "eliminado", nullable = false)
    private boolean eliminado = false;

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    /**
     * Callback JPA invocado antes de persistir la entidad por primera vez.
     * Asigna {@code fechaCreacion} con la marca de tiempo actual en UTC.
     */
    @PrePersist
    protected void onPrePersist() {
        this.fechaCreacion = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Constructor sin argumentos requerido por JPA. */
    public Guia() {
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    /**
     * Retorna el identificador único (UUID v4) de la guía.
     *
     * @return UUID de la guía; {@code null} antes de la primera persistencia.
     */
    public UUID getGuiaId() {
        return guiaId;
    }

    /**
     * Establece el identificador único de la guía.
     * Normalmente no debe usarse directamente; el UUID es auto-generado por JPA.
     *
     * @param guiaId UUID a asignar.
     */
    public void setGuiaId(UUID guiaId) {
        this.guiaId = guiaId;
    }

    /**
     * Retorna el identificador del transportista asociado a la guía.
     *
     * @return transportistaId alfanumérico, máx. 50 caracteres.
     */
    public String getTransportistaId() {
        return transportistaId;
    }

    /**
     * Establece el identificador del transportista.
     *
     * @param transportistaId identificador alfanumérico, máx. 50 caracteres.
     */
    public void setTransportistaId(String transportistaId) {
        this.transportistaId = transportistaId;
    }

    /**
     * Retorna la fecha de envío de la guía.
     *
     * @return fecha de envío en formato ISO 8601 ({@code YYYY-MM-DD}).
     */
    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    /**
     * Establece la fecha de envío de la guía.
     *
     * @param fechaEnvio fecha en formato ISO 8601 ({@code YYYY-MM-DD}).
     */
    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    /**
     * Retorna el nombre o razón social del destinatario.
     *
     * @return destinatario, máx. 255 caracteres.
     */
    public String getDestinatario() {
        return destinatario;
    }

    /**
     * Establece el nombre del destinatario.
     *
     * @param destinatario nombre o razón social, máx. 255 caracteres.
     */
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    /**
     * Retorna la dirección de destino de la carga.
     *
     * @return dirección completa, máx. 500 caracteres.
     */
    public String getDireccionDestino() {
        return direccionDestino;
    }

    /**
     * Establece la dirección de destino.
     *
     * @param direccionDestino dirección completa, máx. 500 caracteres.
     */
    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    /**
     * Retorna el peso de la carga en kilogramos.
     *
     * @return peso en kg, o {@code null} si no fue especificado.
     */
    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    /**
     * Establece el peso de la carga en kilogramos.
     *
     * @param pesoKg peso en kg; puede ser {@code null}.
     */
    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    /**
     * Retorna la descripción del contenido de la carga.
     *
     * @return descripción, máx. 1000 caracteres, o {@code null} si no fue especificada.
     */
    public String getDescripcionCarga() {
        return descripcionCarga;
    }

    /**
     * Establece la descripción del contenido de la carga.
     *
     * @param descripcionCarga descripción, máx. 1000 caracteres; puede ser {@code null}.
     */
    public void setDescripcionCarga(String descripcionCarga) {
        this.descripcionCarga = descripcionCarga;
    }

    /**
     * Retorna las observaciones adicionales del envío.
     *
     * @return observaciones, máx. 2000 caracteres, o {@code null} si no fueron especificadas.
     */
    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Establece las observaciones adicionales del envío.
     *
     * @param observaciones texto libre, máx. 2000 caracteres; puede ser {@code null}.
     */
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    /**
     * Retorna el estado actual del ciclo de vida de la guía.
     *
     * @return estado actual.
     */
    public EstadoGuia getEstado() {
        return estado;
    }

    /**
     * Establece el estado del ciclo de vida de la guía.
     *
     * @param estado nuevo estado; no debe ser {@code null}.
     */
    public void setEstado(EstadoGuia estado) {
        this.estado = estado;
    }

    /**
     * Retorna la URL del objeto almacenado en Amazon S3.
     *
     * @return URL S3, o {@code null} si la guía aún no ha sido subida.
     */
    public String getUrlS3() {
        return urlS3;
    }

    /**
     * Establece la URL del objeto en Amazon S3.
     *
     * @param urlS3 URL completa del objeto S3; máx. 1000 caracteres.
     */
    public void setUrlS3(String urlS3) {
        this.urlS3 = urlS3;
    }

    /**
     * Retorna la fecha y hora de creación de la guía en UTC.
     *
     * @return instante de creación; nunca {@code null} después de la primera persistencia.
     */
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Establece la fecha y hora de creación.
     * Normalmente no debe usarse directamente; el valor es gestionado por {@link #onPrePersist()}.
     *
     * @param fechaCreacion instante de creación en UTC.
     */
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Retorna la fecha y hora de la última modificación de la guía en UTC.
     *
     * @return instante de última modificación, o {@code null} si nunca fue modificada.
     */
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    /**
     * Establece la fecha y hora de la última modificación.
     * Debe asignarse con {@code Instant.now()} en cada operación de actualización exitosa.
     *
     * @param fechaActualizacion instante de modificación en UTC.
     */
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    /**
     * Indica si la guía ha sido eliminada de forma lógica (soft delete).
     *
     * @return {@code true} si la guía está eliminada; {@code false} en caso contrario.
     */
    public boolean isEliminado() {
        return eliminado;
    }

    /**
     * Establece el indicador de eliminación lógica.
     *
     * @param eliminado {@code true} para marcar la guía como eliminada.
     */
    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
}
