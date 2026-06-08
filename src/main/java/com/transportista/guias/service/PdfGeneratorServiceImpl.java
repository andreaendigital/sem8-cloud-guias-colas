package com.transportista.guias.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.transportista.guias.exception.S3UploadException;
import com.transportista.guias.model.EstadoGuia;
import com.transportista.guias.model.Guia;
import com.transportista.guias.repository.GuiaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de generación y subida asíncrona de PDFs para las
 * Guías de Despacho.
 *
 * <p>Esta clase utiliza <b>Apache PDFBox</b> para construir el documento PDF con todos
 * los campos de la guía y un código QR (generado con <b>ZXing</b>) que apunta a la URL
 * de descarga. El flujo completo de la operación es:</p>
 * <ol>
 *   <li>Crear el PDF con todos los campos de la guía y el código QR.</li>
 *   <li>Escribir el archivo temporalmente en el sistema de ficheros EFS bajo la ruta
 *       {@code {EFS_MOUNT_PATH}/{guiaId}.pdf} y actualizar el estado a
 *       {@link EstadoGuia#GENERADA}.</li>
 *   <li>Subir el PDF a Amazon S3 con la clave
 *       {@code /{YYYYMM}/{transportistaId}/{guiaId}.pdf}.</li>
 *   <li>Actualizar el estado a {@link EstadoGuia#SUBIDA} y persistir la {@code urlS3}.</li>
 *   <li>Eliminar el archivo temporal de EFS.</li>
 * </ol>
 *
 * <p>Toda la operación ocurre en un hilo del pool {@code pdfExecutor} definido en
 * {@code AsyncConfig}, de modo que el endpoint de creación responde de forma inmediata
 * (HTTP 201) sin bloquear al cliente.</p>
 *
 * <p><b>Estrategia de errores:</b></p>
 * <ul>
 *   <li>Error en generación de PDF o escritura EFS → log ERROR, guía permanece en
 *       {@code BORRADOR}.</li>
 *   <li>Error en subida S3 → log ERROR, estado actualizado a
 *       {@link EstadoGuia#ERROR_SUBIDA}.</li>
 *   <li>Error al borrar archivo EFS post-subida → log WARN, estado {@code SUBIDA}
 *       no se revierte.</li>
 * </ul>
 *
 * <p><b>Requisitos cubiertos:</b> 1.1, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2,
 * 11.1, 11.2, 11.3, 11.4, 11.5, 11.6</p>
 */
@Service
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorServiceImpl.class);

    private final GuiaRepository guiaRepository;
    private final S3StorageService s3StorageService;

    /** Ruta base del punto de montaje EFS, configurable mediante {@code efs.mount.path}. */
    @Value("${efs.mount.path}")
    private String efsMountPath;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param guiaRepository   repositorio JPA para persistir cambios de estado de la guía
     * @param s3StorageService servicio de operaciones sobre Amazon S3
     */
    public PdfGeneratorServiceImpl(GuiaRepository guiaRepository,
                                   S3StorageService s3StorageService) {
        this.guiaRepository = guiaRepository;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Genera el PDF de la guía de forma asíncrona, lo escribe temporalmente en EFS
     * y lo sube a Amazon S3, actualizando el estado de la guía en cada paso.
     *
     * <p>El método se ejecuta en el pool {@code pdfExecutor} gracias a la anotación
     * {@link Async @Async("pdfExecutor")} y retorna inmediatamente al llamador con un
     * {@link CompletableFuture} ya completado. Los errores internos se manejan dentro
     * del método y nunca se propagan al hilo HTTP original.</p>
     *
     * <p><b>Flujo:</b></p>
     * <ol>
     *   <li>Genera el PDF usando PDFBox e incluye un código QR con la URL de descarga
     *       ({@code /api/v1/guias/{guiaId}/download}).</li>
     *   <li>Actualiza el estado de la guía a {@link EstadoGuia#GENERADA} y persiste.</li>
     *   <li>Construye la clave S3: {@code /{YYYYMM}/{transportistaId}/{guiaId}.pdf}.</li>
     *   <li>Llama a {@link S3StorageService#uploadFile(Path, String)} para subir el PDF.</li>
     *   <li>Actualiza el estado a {@link EstadoGuia#SUBIDA} y persiste la {@code urlS3}.</li>
     *   <li>Intenta eliminar el archivo temporal de EFS.</li>
     * </ol>
     *
     * @param guia guía de despacho cuyo PDF debe generarse y subirse; no debe ser
     *             {@code null} y debe tener al menos {@code guiaId},
     *             {@code transportistaId} y {@code fechaEnvio} establecidos.
     * @return {@link CompletableFuture}{@code <Void>} completado cuando el proceso
     *         asíncrono termina (con éxito o con excepción interna manejada).
     */
    @Async("pdfExecutor")
    @Override
    public CompletableFuture<Void> generarYSubir(Guia guia) {
        UUID guiaId = guia.getGuiaId();
        Path efsPdfPath = Paths.get(efsMountPath, guiaId.toString() + ".pdf");

        try {
            // 1. Crear PDF usando PDFBox
            generarPdf(guia, efsPdfPath);

            // 2. Actualizar estado a GENERADA
            guia.setEstado(EstadoGuia.GENERADA);
            guiaRepository.save(guia);

            // 3. Construir clave S3: /{YYYYMM}/{transportistaId}/{guiaId}.pdf
            String yyyymm = guia.getFechaEnvio().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String s3Key = "/" + yyyymm + "/" + guia.getTransportistaId() + "/" + guiaId + ".pdf";

            // 4. Subir a S3
            s3StorageService.uploadFile(efsPdfPath, s3Key);

            // 5. Actualizar estado a SUBIDA y guardar urlS3
            guia.setEstado(EstadoGuia.SUBIDA);
            guia.setUrlS3(s3Key);
            guiaRepository.save(guia);

            // 6. Eliminar archivo de EFS
            try {
                Files.deleteIfExists(efsPdfPath);
            } catch (IOException e) {
                log.warn("No se pudo eliminar el archivo temporal de EFS para guia {}: {}",
                        guiaId, e.getMessage());
            }

        } catch (S3UploadException e) {
            log.error("Error al subir PDF a S3 para guia {}: {}", guiaId, e.getMessage());
            guia.setEstado(EstadoGuia.ERROR_SUBIDA);
            try {
                guiaRepository.save(guia);
            } catch (Exception ex) {
                log.error("Error al persistir estado ERROR_SUBIDA para guia {}: {}",
                        guiaId, ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al generar PDF para guia {}: {}", guiaId, e.getMessage(), e);
            // La guía permanece en estado BORRADOR
        }

        return CompletableFuture.completedFuture(null);
    }

    // -------------------------------------------------------------------------
    // Métodos privados auxiliares
    // -------------------------------------------------------------------------

    /**
     * Crea el documento PDF para la guía indicada y lo escribe en {@code outputPath}.
     *
     * <p>El PDF incluye una portada A4 con los campos de la guía y un código QR
     * posicionado en la esquina superior derecha generado con ZXing.</p>
     *
     * @param guia       guía cuyos datos se escriben en el PDF
     * @param outputPath ruta de destino del archivo PDF (inclusive nombre de fichero)
     * @throws IOException si ocurre un error de escritura en el sistema de archivos
     */
    private void generarPdf(Guia guia, Path outputPath) throws IOException {
        // Crear directorio padre si no existe
        Files.createDirectories(outputPath.getParent());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float y = 750;
                final float margin = 50;
                final float lineHeight = 20;

                // Título
                cs.beginText();
                cs.setFont(font, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("GUIA DE DESPACHO");
                cs.endText();
                y -= lineHeight * 2;

                // Campos obligatorios
                writeField(cs, fontNormal, margin, y, "Guia ID: " + guia.getGuiaId());
                y -= lineHeight;
                writeField(cs, fontNormal, margin, y, "Transportista: " + guia.getTransportistaId());
                y -= lineHeight;
                writeField(cs, fontNormal, margin, y, "Fecha Envio: " + guia.getFechaEnvio());
                y -= lineHeight;
                writeField(cs, fontNormal, margin, y, "Destinatario: " + guia.getDestinatario());
                y -= lineHeight;
                writeField(cs, fontNormal, margin, y, "Direccion Destino: " + guia.getDireccionDestino());
                y -= lineHeight;

                // Campos opcionales
                if (guia.getPesoKg() != null) {
                    writeField(cs, fontNormal, margin, y, "Peso Kg: " + guia.getPesoKg());
                    y -= lineHeight;
                }
                if (guia.getDescripcionCarga() != null) {
                    writeField(cs, fontNormal, margin, y,
                            "Descripcion Carga: " + guia.getDescripcionCarga());
                    y -= lineHeight;
                }
                if (guia.getObservaciones() != null) {
                    writeField(cs, fontNormal, margin, y,
                            "Observaciones: " + guia.getObservaciones());
                }
            }

            // Insertar código QR como imagen en el PDF
            String qrUrl = "/api/v1/guias/" + guia.getGuiaId() + "/download";
            agregarQR(document, page, qrUrl);

            document.save(outputPath.toFile());
        }
    }

    /**
     * Escribe una línea de texto en la posición indicada del stream de contenido PDF.
     *
     * @param cs   stream de contenido de la página PDF
     * @param font fuente con la que se renderiza el texto
     * @param x    posición horizontal (puntos desde el margen izquierdo)
     * @param y    posición vertical (puntos desde la base de la página)
     * @param text texto a renderizar
     * @throws IOException si ocurre un error al escribir en el stream
     */
    private void writeField(PDPageContentStream cs, PDType1Font font,
                            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * Genera un código QR con ZXing para el contenido dado y lo inserta en la página
     * PDF en la esquina superior derecha (coordenadas aproximadas: x=450, y=650).
     *
     * <p>Si ocurre cualquier error durante la generación del QR (p. ej. tamaño
     * inválido, error de escritura en el stream), el problema se registra como
     * {@code WARN} y el PDF se guarda igualmente sin el código QR.</p>
     *
     * @param document documento PDF al que se añade la imagen QR
     * @param page     página del documento donde se dibujará el QR
     * @param content  texto o URL a codificar en el código QR
     */
    private void agregarQR(PDDocument document, PDPage page, String content) {
        try {
            int qrSize = 100;
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);

            // Convertir BufferedImage a PDImageXObject
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, qrImage);

            try (PDPageContentStream cs = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                cs.drawImage(pdImage, 450, 650, qrSize, qrSize);
            }
        } catch (Exception e) {
            log.warn("No se pudo generar el codigo QR para el PDF: {}", e.getMessage());
        }
    }
}
