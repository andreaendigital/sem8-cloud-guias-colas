package com.transportista.guias.service;

import com.transportista.guias.exception.S3UploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Implementación de {@link S3StorageService} que delega en AWS SDK v2 para
 * ejecutar operaciones sobre Amazon S3.
 *
 * <p>Características principales:</p>
 * <ul>
 *   <li><b>Subida con reintentos</b>: {@link #uploadFile(Path, String)} se anota con
 *       {@code @Retryable} para reintentar hasta 3 veces ante {@link SdkClientException}
 *       o {@link S3Exception} con un back-off de 2 segundos entre intentos.</li>
 *   <li><b>Recuperación de error</b>: {@link #recoverUpload(Exception, Path, String)} actúa
 *       como método {@code @Recover} y lanza {@link S3UploadException} tras agotar los
 *       reintentos, registrando el error en el log.</li>
 *   <li><b>Eliminación idempotente</b>: {@link #deleteObject(String)} ignora silenciosamente
 *       el error {@code NoSuchKey} para soportar operaciones idempotentes.</li>
 *   <li><b>URLs pre-firmadas</b>: {@link #generatePresignedUrl(String, Duration)} delega en
 *       {@link S3Presigner} para generar URLs de descarga directa con tiempo de validez
 *       configurable.</li>
 * </ul>
 *
 * <p><strong>Requisitos cubiertos:</strong> 2.1, 2.2, 2.3, 2.4, 5.2, 7.3</p>
 */
@Service
public class S3StorageServiceImpl implements S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    /** Cliente AWS SDK v2 para operaciones directas sobre S3 (put, delete). */
    private final S3Client s3Client;

    /** Pre-firmador AWS SDK v2 para generar URLs pre-firmadas de descarga. */
    private final S3Presigner s3Presigner;

    /**
     * Nombre del bucket S3 destino, inyectado desde la propiedad {@code aws.s3.bucket}.
     * En desarrollo local, el valor por defecto es {@code guias-local} (definido en
     * {@code application.properties}).
     */
    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Construye la implementación inyectando las dependencias de AWS.
     *
     * @param s3Client    cliente S3 configurado en {@code AwsS3Config}
     * @param s3Presigner pre-firmador S3 configurado en {@code AwsS3Config}
     */
    public S3StorageServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Sube el archivo local indicado al bucket S3 usando la clave {@code s3Key}.
     *
     * <p>El método se reintenta automáticamente hasta 3 veces con un back-off de
     * 2 000 ms ante cualquier {@link SdkClientException} (errores de red/timeout) o
     * {@link S3Exception} (errores del servicio S3). Si los 3 intentos fallan, el
     * método {@link #recoverUpload(Exception, Path, String)} entra en acción.</p>
     *
     * <p>Errores no reintentables (p. ej. {@code NoSuchBucketException}) lanzarán
     * la excepción directamente sin pasar por el back-off.</p>
     *
     * @param localPath ruta local del archivo a subir (tipicamente en el directorio EFS)
     * @param s3Key     clave destino en S3, con formato {@code YYYYMM/transportistaId/guiaId.pdf}
     * @return la misma {@code s3Key} recibida, confirmando la clave del objeto creado
     * @throws S3UploadException si se agotan los reintentos (delegado a {@link #recoverUpload})
     */
    @Override
    @Retryable(retryFor = {SdkClientException.class, S3Exception.class},
               maxAttempts = 3,
               backoff = @Backoff(delay = 2000))
    public String uploadFile(Path localPath, String s3Key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(localPath));
        log.info("Archivo subido exitosamente a S3 con clave: {}", s3Key);
        return s3Key;
    }

    /**
     * Método de recuperación invocado por Spring Retry cuando {@link #uploadFile(Path, String)}
     * falla tras agotar los {@code maxAttempts} configurados.
     *
     * <p>Registra el error a nivel {@code ERROR} en el log e inmediatamente lanza
     * {@link S3UploadException} para que el llamador (o el {@code GlobalExceptionHandler})
     * pueda manejar el fallo apropiadamente (HTTP 502).</p>
     *
     * <p><b>Nota:</b> La firma del método {@code @Recover} debe coincidir con la del método
     * reintentable: mismo tipo de retorno y mismos parámetros, precedidos por la excepción.</p>
     *
     * @param e         excepción que provocó el agotamiento de reintentos
     * @param localPath ruta local del archivo que no pudo subirse
     * @param s3Key     clave S3 de destino que no pudo crearse
     * @return nunca retorna; siempre lanza {@link S3UploadException}
     * @throws S3UploadException siempre, con el mensaje y la causa de la excepción original
     */
    @Recover
    public String recoverUpload(Exception e, Path localPath, String s3Key) {
        log.error("S3 upload failed for key {} after retries: {}", s3Key, e.getMessage());
        throw new S3UploadException("Error al subir archivo a S3: " + e.getMessage(), e);
    }

    /**
     * Elimina el objeto con clave {@code s3Key} del bucket S3.
     *
     * <p>La operación es idempotente: si el objeto no existe ({@code NoSuchKey}),
     * el error se ignora y se registra un mensaje informativo. Cualquier otro
     * {@link S3Exception} se registra como advertencia pero no se propaga al
     * llamador, para no interrumpir el flujo de eliminación lógica de guías.</p>
     *
     * @param s3Key clave del objeto a eliminar en S3
     */
    @Override
    public void deleteObject(String s3Key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Objeto S3 eliminado con clave: {}", s3Key);
        } catch (S3Exception e) {
            if ("NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
                log.info("S3 object not found during delete, ignoring: {}", s3Key);
            } else {
                log.warn("Error deleting S3 object {}: {}", s3Key, e.getMessage());
            }
        }
    }

    /**
     * Genera una URL pre-firmada para descarga directa del objeto identificado por {@code s3Key}.
     *
     * <p>La URL generada permite al cliente descargar el PDF desde S3 sin necesidad de
     * credenciales AWS propias. Es válida durante el tiempo indicado en {@code validity}
     * (p. ej. {@code Duration.ofMinutes(15)}) y se construye mediante {@link S3Presigner}.</p>
     *
     * @param s3Key    clave del objeto en S3 para el que se genera la URL pre-firmada
     * @param validity duración de validez de la URL pre-firmada
     * @return {@link URL} pre-firmada lista para ser redirigida al cliente (HTTP 302)
     */
    @Override
    public URL generatePresignedUrl(String s3Key, Duration validity) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(validity)
                .getObjectRequest(getRequest)
                .build();
        URL url = s3Presigner.presignGetObject(presignRequest).url();
        log.debug("URL pre-firmada generada para clave {}: {}", s3Key, url);
        return url;
    }
}
