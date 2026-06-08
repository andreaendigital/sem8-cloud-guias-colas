package com.transportista.guias.repository;

import com.transportista.guias.model.Guia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Guia}.
 *
 * <p>Extiende {@link JpaRepository} para proveer operaciones CRUD estándar
 * sobre la tabla {@code guias}, además de consultas derivadas específicas
 * del dominio de gestión de guías de despacho.</p>
 *
 * <p>Spring Data JPA genera la implementación en tiempo de ejecución a partir
 * de los nombres de los métodos declarados en esta interfaz.</p>
 *
 * <p>Relacionado con los requisitos: 8.1 (consulta paginada por transportista y
 * rango de fechas), 8.6 (exclusión de guías eliminadas en la consulta).</p>
 */
@Repository
public interface GuiaRepository extends JpaRepository<Guia, UUID> {

    /**
     * Busca guías activas (no eliminadas) de un transportista en un rango de fechas,
     * con soporte de paginación.
     *
     * <p>El método excluye automáticamente los registros con {@code eliminado = true},
     * por lo que solo devuelve guías disponibles para el transportista indicado cuya
     * {@code fechaEnvio} se encuentre dentro del intervalo cerrado
     * [{@code inicio}, {@code fin}].</p>
     *
     * @param transportistaId identificador alfanumérico del transportista; no debe ser
     *                        {@code null} ni estar en blanco.
     * @param inicio          fecha de inicio del rango de búsqueda (inclusive),
     *                        en formato ISO 8601 ({@code YYYY-MM-DD}).
     * @param fin             fecha de fin del rango de búsqueda (inclusive),
     *                        en formato ISO 8601 ({@code YYYY-MM-DD}).
     * @param pageable        parámetros de paginación y ordenación; no debe ser
     *                        {@code null}.
     * @return página de guías que cumplen los criterios, nunca {@code null};
     *         puede ser una página vacía si no hay resultados.
     */
    Page<Guia> findByTransportistaIdAndFechaEnvioBetweenAndEliminadoFalse(
            String transportistaId,
            LocalDate inicio,
            LocalDate fin,
            Pageable pageable
    );
}
