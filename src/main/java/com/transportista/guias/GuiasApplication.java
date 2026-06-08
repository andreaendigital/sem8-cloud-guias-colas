package com.transportista.guias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal del microservicio de Gestión de Guías de Despacho.
 *
 * <p>Activa las funcionalidades de procesamiento asíncrono ({@code @EnableAsync}) y
 * reintentos automáticos ({@code @EnableRetry}) requeridas por el diseño.</p>
 */
@SpringBootApplication
@EnableAsync
@EnableRetry
public class GuiasApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuiasApplication.class, args);
    }
}
