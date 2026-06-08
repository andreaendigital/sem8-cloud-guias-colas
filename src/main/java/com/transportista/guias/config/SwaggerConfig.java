package com.transportista.guias.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Springdoc para la documentación interactiva de la API REST.
 * Accesible en /swagger-ui/index.html y /v3/api-docs.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Define los metadatos de la API expuestos en la documentación OpenAPI 3.0.
     *
     * @return instancia de {@link OpenAPI} con título, versión y descripción del servicio.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestión de Guías de Despacho API")
                        .version("1.0.0")
                        .description("API REST para gestión del ciclo de vida de Guías de Despacho"));
    }
}
