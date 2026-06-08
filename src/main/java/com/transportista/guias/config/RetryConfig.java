package com.transportista.guias.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Habilita el soporte de Spring Retry en el contexto de la aplicación.
 *
 * <p>Al anotar esta clase con {@link EnableRetry}, Spring procesa las anotaciones
 * {@code @Retryable} y {@code @Recover} declaradas en los servicios (p. ej.
 * {@code S3StorageServiceImpl}), generando los proxies AOP necesarios para
 * reintentar automáticamente operaciones fallidas con back-off configurable.</p>
 *
 * <p><strong>Requisitos cubiertos:</strong> 2.3, 2.4 — Reintentos automáticos
 * en subida a S3 con un máximo de 3 intentos y back-off de 2 segundos.</p>
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
