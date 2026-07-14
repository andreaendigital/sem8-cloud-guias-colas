package com.transportista.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del API Gateway.
 *
 * <p>Actúa como punto de entrada único para todos los microservicios del monorepo.
 * Valida el token JWT de Azure AD B2C antes de reenviar cada request al
 * microservicio correspondiente:</p>
 * <ul>
 *   <li>{@code /api/v1/guias/**}    → guias-service (puerto 8090)</li>
 *   <li>{@code /api/v1/producer/**} → producer      (puerto 8081)</li>
 *   <li>{@code /api/v1/consumer/**} → consumer      (puerto 8082)</li>
 * </ul>
 *
 * <p>Spring Cloud Gateway es reactivo (WebFlux), por lo que NO debe agregarse
 * {@code spring-boot-starter-web} al classpath.</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
