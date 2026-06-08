package com.transportista.guias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuración de los beans de AWS S3 para el sistema de gestión de guías de despacho.
 *
 * <p>Las credenciales ({@code AWS_ACCESS_KEY_ID} y {@code AWS_SECRET_ACCESS_KEY}) se leen
 * automáticamente desde variables de entorno por medio de {@link EnvironmentVariableCredentialsProvider}.
 * La región se inyecta desde la propiedad {@code aws.region} (respaldada por la variable
 * de entorno {@code AWS_REGION}).</p>
 *
 * <p>Cuando la propiedad {@code aws.endpoint.override} no está vacía, tanto el {@link S3Client}
 * como el {@link S3Presigner} apuntan a ese endpoint (p. ej. LocalStack en desarrollo local)
 * con acceso en modo path-style habilitado.</p>
 *
 * <p><strong>Requisitos cubiertos:</strong> 2.5 (S3Client configurable), 2.6 (soporte LocalStack).</p>
 */
@Configuration
public class AwsS3Config {

    /**
     * Región AWS leída desde la propiedad {@code aws.region}.
     * Valor por defecto: {@code us-east-1} (definido en {@code application.properties}).
     */
    @Value("${aws.region}")
    private String awsRegion;

    /**
     * URL del endpoint alternativo para LocalStack u otro emulador de S3.
     * Cuando está vacío (valor por defecto) se usa el endpoint estándar de AWS.
     * Corresponde a la propiedad {@code aws.endpoint.override}.
     */
    @Value("${aws.endpoint.override:}")
    private String endpointOverride;

    /**
     * Crea el bean {@link S3Client} utilizado para operaciones de put/delete de objetos en S3.
     *
     * <p>Configuración aplicada:</p>
     * <ul>
     *   <li>Credenciales: {@link EnvironmentVariableCredentialsProvider} (lee
     *       {@code AWS_ACCESS_KEY_ID} y {@code AWS_SECRET_ACCESS_KEY} del entorno).</li>
     *   <li>Región: valor de la propiedad {@code aws.region}.</li>
     *   <li>Si {@code aws.endpoint.override} no está en blanco: se configura el endpoint
     *       personalizado y se activa {@code pathStyleAccessEnabled} para compatibilidad
     *       con LocalStack.</li>
     * </ul>
     *
     * @return instancia configurada de {@link S3Client}
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(awsRegion));

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .serviceConfiguration(
                           S3Configuration.builder()
                                   .pathStyleAccessEnabled(true)
                                   .build()
                   );
        }

        return builder.build();
    }

    /**
     * Crea el bean {@link S3Presigner} utilizado para generar URLs pre-firmadas de descarga.
     *
     * <p>Aplica la misma configuración de credenciales, región y endpoint override que
     * {@link #s3Client()}, garantizando coherencia entre las operaciones directas sobre S3
     * y las URLs generadas.</p>
     *
     * @return instancia configurada de {@link S3Presigner}
     */
    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(awsRegion));

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   .serviceConfiguration(
                           S3Configuration.builder()
                                   .pathStyleAccessEnabled(true)
                                   .build()
                   );
        }

        return builder.build();
    }
}
