package com.transportista.guias.service;

import com.transportista.guias.dto.ActualizarGuiaRequestDTO;
import com.transportista.guias.dto.CrearGuiaRequestDTO;
import com.transportista.guias.dto.GuiaListItemDTO;
import com.transportista.guias.dto.GuiaResponseDTO;
import com.transportista.guias.dto.PaginatedResponseDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/**
 * Contrato principal de negocio para la gestión del ciclo de vida de las
 * Guías de Despacho.
 *
 * <p>Define las operaciones CRUD y de flujo de trabajo disponibles sobre las
 * guías: creación, subida a S3, descarga, actualización, eliminación lógica y
 * consulta paginada. La implementación concreta es {@code GuiaServiceImpl}.</p>
 *
 * <p>La creación de guías dispara de forma asíncrona (no bloqueante) la generación
 * del PDF y su posterior subida a Amazon S3, por lo que el estado inicial devuelto
 * siempre es {@code BORRADOR}.</p>
 *
 * <p><b>Requisitos relacionados:</b> 3.1, 3.6, 3.7, 3.8, 5.1, 6.1, 7.1, 8.1</p>
 */
public interface GuiaService {

    /**
     * Crea una nueva Guía de Despacho a partir de los datos proporcionados.
     *
     * <p>Persiste la guía en base de datos con estado {@code BORRADOR} y, de forma
     * asíncrona y no bloqueante, dispara la generación del PDF y su subida a S3.
     * Devuelve la representación completa de la guía recién creada.</p>
     *
     * @param dto datos de creación de la guía; no debe ser {@code null} y debe
     *            pasar las validaciones de Bean Validation.
     * @return {@link GuiaResponseDTO} con todos los campos de la guía creada,
     *         incluyendo el {@code guiaId} generado y el estado {@code BORRADOR}.
     * @throws com.transportista.guias.exception.GuiaNotFoundException si no puede
     *         persistirse la guía.
     */
    GuiaResponseDTO crearGuia(CrearGuiaRequestDTO dto);

    /**
     * Dispara manualmente la subida del PDF de una guía a Amazon S3.
     *
     * <p>Se utiliza cuando la subida automática (asíncrona) falló y la guía quedó
     * en estado {@code ERROR_SUBIDA} o {@code GENERADA}. Requiere que el PDF ya
     * exista en EFS.</p>
     *
     * @param guiaId identificador UUID de la guía a subir; no debe ser {@code null}.
     * @return {@link GuiaResponseDTO} actualizado con el estado {@code SUBIDA} y la
     *         {@code urlS3} generada.
     * @throws com.transportista.guias.exception.GuiaNotFoundException si no existe
     *         una guía con el {@code guiaId} indicado.
     * @throws com.transportista.guias.exception.GuiaNoDisponibleException si la guía
     *         ya está en estado {@code SUBIDA} o {@code ELIMINADA}.
     * @throws com.transportista.guias.exception.S3UploadException si la subida a S3
     *         falla tras los reintentos configurados.
     */
    GuiaResponseDTO uploadGuia(UUID guiaId);

    /**
     * Descarga la guía identificada por {@code guiaId} redirigiendo al cliente a la
     * URL pre-firmada de Amazon S3.
     *
     * <p>Valida que el token JWT incluya al transportista de la guía en el claim
     * {@code transportistasPermitidos}. Si la guía está disponible (estado
     * {@code SUBIDA}), genera una URL pre-firmada de corta duración y redirige
     * la respuesta HTTP con código 302.</p>
     *
     * @param guiaId   identificador UUID de la guía a descargar; no debe ser
     *                 {@code null}.
     * @param jwtToken token JWT del usuario solicitante (valor del header
     *                 {@code Authorization} sin el prefijo {@code Bearer }).
     * @param response objeto {@link HttpServletResponse} usado para redirigir al
     *                 cliente a la URL pre-firmada.
     * @throws com.transportista.guias.exception.GuiaNotFoundException si no existe
     *         una guía con el {@code guiaId} indicado.
     * @throws com.transportista.guias.exception.GuiaNoDisponibleException si la guía
     *         no está en estado {@code SUBIDA}.
     * @throws org.springframework.security.access.AccessDeniedException si el JWT no
     *         autoriza la descarga para el transportista de la guía.
     */
    void downloadGuia(UUID guiaId, String jwtToken, HttpServletResponse response);

    /**
     * Actualiza los campos permitidos de una Guía de Despacho existente.
     *
     * <p>Solo se modifican los campos presentes en {@link ActualizarGuiaRequestDTO}
     * ({@code destinatario}, {@code direccionDestino}, {@code pesoKg},
     * {@code descripcionCarga}, {@code observaciones}). Cualquier otro campo en el
     * cuerpo JSON produce HTTP 400. Tras la actualización, el estado de la guía
     * regresa a {@code BORRADOR} para forzar la regeneración del PDF.</p>
     *
     * @param guiaId identificador UUID de la guía a actualizar; no debe ser
     *               {@code null}.
     * @param dto    datos de actualización; no debe ser {@code null}.
     * @return {@link GuiaResponseDTO} con los campos actualizados y la nueva
     *         {@code fechaActualizacion}.
     * @throws com.transportista.guias.exception.GuiaNotFoundException si no existe
     *         una guía con el {@code guiaId} indicado.
     * @throws com.transportista.guias.exception.GuiaYaEliminadaException si la guía
     *         ya se encuentra en estado {@code ELIMINADA}.
     */
    GuiaResponseDTO actualizarGuia(UUID guiaId, ActualizarGuiaRequestDTO dto);

    /**
     * Realiza la eliminación lógica (soft delete) de una Guía de Despacho.
     *
     * <p>Marca la guía con {@code eliminado = true} y cambia su estado a
     * {@code ELIMINADA}. El registro permanece en base de datos. Solo usuarios con
     * rol {@code ROLE_ADMIN} pueden ejecutar esta operación.</p>
     *
     * @param guiaId   identificador UUID de la guía a eliminar; no debe ser
     *                 {@code null}.
     * @param jwtToken token JWT del usuario solicitante, usado para verificar el rol
     *                 {@code ROLE_ADMIN}.
     * @throws com.transportista.guias.exception.GuiaNotFoundException si no existe
     *         una guía con el {@code guiaId} indicado.
     * @throws com.transportista.guias.exception.GuiaYaEliminadaException si la guía
     *         ya se encuentra en estado {@code ELIMINADA}.
     * @throws org.springframework.security.access.AccessDeniedException si el JWT no
     *         contiene el rol {@code ROLE_ADMIN}.
     */
    void eliminarGuia(UUID guiaId, String jwtToken);

    /**
     * Consulta las guías de despacho de un transportista con filtros opcionales y
     * paginación.
     *
     * <p>Devuelve únicamente guías con {@code eliminado = false}. El parámetro
     * {@code fecha} corresponde al mes en formato {@code YYYYMM}; el sistema lo
     * convierte al rango de fechas del mes completo para la consulta. Los parámetros
     * {@code page} y {@code size} deben respetar los límites 0 ≤ page y
     * 1 ≤ size ≤ 100.</p>
     *
     * @param transportistaId identificador del transportista cuyos registros se
     *                        consultan; obligatorio, no debe ser {@code null} ni
     *                        vacío.
     * @param fecha           mes de consulta en formato {@code YYYYMM} (p. ej.,
     *                        {@code "202507"}); puede ser {@code null} para no
     *                        filtrar por mes.
     * @param page            número de página (base 0); debe ser ≥ 0.
     * @param size            tamaño de página; debe estar entre 1 y 100 inclusive.
     * @return {@link PaginatedResponseDTO} con la lista de {@link GuiaListItemDTO}
     *         y los metadatos de paginación.
     * @throws IllegalArgumentException si {@code transportistaId} es nulo/vacío,
     *         {@code fecha} tiene formato inválido, o los parámetros de paginación
     *         están fuera de rango.
     */
    PaginatedResponseDTO<GuiaListItemDTO> consultarGuias(
            String transportistaId, String fecha, int page, int size);
}
