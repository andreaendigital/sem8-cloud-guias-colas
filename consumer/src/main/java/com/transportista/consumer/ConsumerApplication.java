package com.transportista.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio Consumer.
 *
 * <p>Escucha mensajes de Guías de Despacho en RabbitMQ ({@code guias.queue})
 * y los persiste en la tabla {@code GUIAS_EVENTOS} de su propia base de datos.
 * También expone un endpoint REST para consultar los eventos procesados.</p>
 */
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
