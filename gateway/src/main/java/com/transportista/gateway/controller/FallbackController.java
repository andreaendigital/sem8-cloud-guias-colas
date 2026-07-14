package com.transportista.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador de fallback del API Gateway.
 *
 * <p>Devuelve una respuesta 503 descriptiva cuando un microservicio no está
 * disponible y el Circuit Breaker activa el fallback.</p>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/guias")
    public ResponseEntity<Map<String, Object>> fallbackGuias() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status", 503,
            "servicio", "guias-service",
            "mensaje", "El servicio de guías no está disponible temporalmente. Intente más tarde.",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/producer")
    public ResponseEntity<Map<String, Object>> fallbackProducer() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status", 503,
            "servicio", "producer",
            "mensaje", "El servicio producer no está disponible temporalmente. Intente más tarde.",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/consumer")
    public ResponseEntity<Map<String, Object>> fallbackConsumer() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status", 503,
            "servicio", "consumer",
            "mensaje", "El servicio consumer no está disponible temporalmente. Intente más tarde.",
            "timestamp", Instant.now().toString()
        ));
    }
}
