package com.transportista.guias.service;

import com.transportista.guias.model.Guia;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio para la generación asíncrona del PDF de una Guía de Despacho.
 *
 * <p>La implementación de esta interfaz utiliza <b>Apache PDFBox</b> para construir
 * el documento PDF y lo almacena temporalmente en el sistema de archivos EFS antes de
 * subirlo a Amazon S3. Toda la operación ocurre en un hilo separado del pool
 * {@code pdfExecutor} definido en {@code AsyncConfig}, de modo que el endpoint de
 * creación de guías responda de forma inmediata (HTTP 201) sin bloquear al cliente.</p>
 *
 * <p>Los errores durante la generación o la subida se registran en el log y actualizan
 * el estado de la guía a {@code ERROR_SUBIDA} cuando corresponde; no se propagan al
 * hilo HTTP original.</p>
 *
 * <p><b>Requisitos relacionados:</b> 3.6, 3.7, 3.8</p>
 */
public interface PdfGeneratorService {

    /**
     * Genera el PDF de la guía de forma asíncrona, lo guarda temporalmente en EFS
     * y lo sube a Amazon S3.
     *
     * <p>El método está anotado con {@link Async @Async("pdfExecutor")} en su
     * implementación, por lo que devuelve de inmediato un {@link CompletableFuture}
     * sin bloquear el hilo del llamador. El flujo interno es:</p>
     * <ol>
     *   <li>Construir el PDF con PDFBox (incluye código QR con el {@code guiaId}).</li>
     *   <li>Escribir el PDF en EFS bajo la ruta {@code {EFS_MOUNT_PATH}/{guiaId}.pdf}
     *       y actualizar el estado a {@code GENERADA}.</li>
     *   <li>Subir el PDF a S3 con la clave {@code /{YYYYMM}/{transportistaId}/{guiaId}.pdf}.</li>
     *   <li>Actualizar el estado a {@code SUBIDA} y almacenar la {@code urlS3} en la base
     *       de datos.</li>
     *   <li>Eliminar el archivo temporal de EFS.</li>
     * </ol>
     *
     * <p>En caso de error en cualquier paso, el estado de la guía se actualiza a
     * {@code ERROR_SUBIDA} (si el fallo ocurre durante la subida a S3) o permanece
     * en {@code BORRADOR} (si el fallo ocurre antes de la escritura en EFS).</p>
     *
     * @param guia guía de despacho cuyo PDF se debe generar y subir; no debe ser
     *             {@code null} y debe tener al menos {@code guiaId} y
     *             {@code transportistaId} establecidos.
     * @return {@link CompletableFuture}{@code <Void>} que completa cuando el proceso
     *         asíncrono finaliza (con éxito o con excepción interna manejada).
     */
    @Async("pdfExecutor")
    CompletableFuture<Void> generarYSubir(Guia guia);
}
