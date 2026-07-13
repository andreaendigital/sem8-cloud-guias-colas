package com.transportista.guias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración del procesamiento asíncrono para la generación de PDFs.
 *
 * <p>Habilita el soporte de {@code @Async} en el contexto de Spring mediante la
 * anotación {@link EnableAsync} y define el bean {@code pdfExecutor}, un
 * {@link ThreadPoolTaskExecutor} dedicado exclusivamente a la generación de PDFs
 * y su subida a Amazon S3.</p>
 *
 * <p>El tamaño base del pool se configura mediante la propiedad
 * {@code pdf.async.pool.size} (valor por defecto: {@code 4}). El tamaño máximo
 * se establece en el doble del valor base para absorber picos de carga. La cola
 * de tareas pendientes admite hasta 50 trabajos en espera antes de rechazar nuevas
 * solicitudes.</p>
 *
 * <p>El nombre del pool ({@code pdf-async-}) facilita la identificación de los hilos
 * en herramientas de monitoreo y logs.</p>
 *
 * <p><b>Requisitos relacionados:</b> 3.7, 3.8</p>
 *
 * @see org.springframework.scheduling.annotation.Async
 * @see com.transportista.guias.service.PdfGeneratorService
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Tamaño base del pool de hilos para la generación asíncrona de PDFs.
     * Se lee de la propiedad {@code pdf.async.pool.size}; si no está definida,
     * el valor por defecto es {@code 4}.
     */
    @Value("${pdf.async.pool.size:4}")
    private int poolSize;

    /**
     * Define el {@link TaskExecutor} utilizado por {@code @Async("pdfExecutor")}
     * en {@link com.transportista.guias.service.PdfGeneratorService}.
     *
     * <p>Configuración del executor:</p>
     * <ul>
     *   <li><b>corePoolSize</b>: {@code poolSize} (valor de la propiedad).</li>
     *   <li><b>maxPoolSize</b>: {@code poolSize * 2} para absorber picos de demanda.</li>
     *   <li><b>queueCapacity</b>: {@code 50} tareas en cola antes de rechazar nuevas.</li>
     *   <li><b>threadNamePrefix</b>: {@code "pdf-async-"} para identificación en logs.</li>
     * </ul>
     *
     * @return instancia inicializada de {@link ThreadPoolTaskExecutor}.
     */
    @Bean("pdfExecutor")
    public TaskExecutor pdfExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("pdf-async-");
        executor.initialize();
        return executor;
    }
}
