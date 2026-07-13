package com.transportista.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio Producer.
 *
 * <p>Recibe peticiones REST y publica mensajes de Guías de Despacho
 * en RabbitMQ (cola principal {@code guias.queue}). Ante fallos
 * de publicación, reencamina a la Dead Letter Queue ({@code guias.error.queue}).</p>
 */
@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}
