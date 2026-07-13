package com.transportista.consumer.repository;

import com.transportista.consumer.model.GuiaDespachoEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para eventos de Guías de Despacho procesados por el Consumer.
 */
@Repository
public interface GuiaDespachoEventRepository extends JpaRepository<GuiaDespachoEvent, Long> {

    /**
     * Encuentra todos los eventos de una guía específica por su UUID de negocio.
     *
     * @param guiaId UUID de la guía
     * @return lista de eventos ordenados por fecha de procesamiento
     */
    List<GuiaDespachoEvent> findByGuiaIdOrderByProcesadoEnDesc(UUID guiaId);

    /**
     * Encuentra todos los eventos de un transportista con paginación.
     *
     * @param transportistaId identificador del transportista
     * @param pageable        parámetros de paginación y ordenación
     * @return página de eventos del transportista
     */
    Page<GuiaDespachoEvent> findByTransportistaId(String transportistaId, Pageable pageable);

    /**
     * Cuenta el total de eventos procesados por tipo de operación.
     *
     * @param operacion tipo de operación (CREAR, ACTUALIZAR, ELIMINAR)
     * @return número de eventos de ese tipo
     */
    long countByOperacion(String operacion);
}
