package com.transportista.guias.service;

import com.transportista.guias.exception.S3UploadException;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Abstracción sobre AWS SDK v2 para operaciones con Amazon S3.
 *
 * <p>Define el contrato de alto nivel para subir archivos PDF al bucket configurado,
 * eliminar objetos y generar URLs pre-firmadas para descarga directa por el cliente.
 * La implementación concreta ({@link S3StorageServiceImpl}) aplica reintentos
 * automáticos con Spring Retry.</p>
 *
 * <p><strong>Requisitos cubiertos:</strong> 2.1, 2.2, 2.3, 2.4, 5.2, 7.3</p>
 */
public interface S3StorageService {

    /**
     * Sube el archivo ubicado en {@code localPath} al bucket S3 usando la clave {@code s3Key}.
     *
     * <p>El método se reintenta hasta 3 veces (con back-off de 2 segundos) ante errores
     * transitorios de red o de servicio S3. Si todos los reintentos fallan, se lanza
     * {@link S3UploadException}.</p>
     *
     * @param localPath ruta local (tipicamente en EFS) del archivo PDF a subir
     * @param s3Key     clave de destino en S3, con el formato {@code /YYYYMM/transportistaId/guiaId.pdf}
     * @return la misma {@code s3Key} que se pasó como argumento, confirmando el objeto creado
     * @throws S3UploadException si la subida falla tras agotar los reintentos configurados
     */
    String uploadFile(Path localPath, String s3Key) throws S3UploadException;

    /**
     * Elimina el objeto con clave {@code s3Key} del bucket S3.
     *
     * <p>Si el objeto no existe ({@code NoSuchKey}), la operación se ignora silenciosamente
     * (operación idempotente). Otros errores se registran en el log pero no se propagan.</p>
     *
     * @param s3Key clave del objeto a eliminar en S3
     */
    void deleteObject(String s3Key);

    /**
     * Genera una URL pre-firmada para descarga directa del objeto identificado por {@code s3Key}.
     *
     * <p>La URL es válida durante el período indicado en {@code validity} y permite al cliente
     * descargar el archivo sin necesidad de credenciales AWS propias.</p>
     *
     * @param s3Key    clave del objeto en S3 para el que se genera la URL
     * @param validity duración de validez de la URL pre-firmada (p. ej. {@code Duration.ofMinutes(15)})
     * @return URL pre-firmada lista para ser redirigida al cliente
     */
    URL generatePresignedUrl(String s3Key, Duration validity);
}
