package com.transportista.guias.dto;

import java.util.List;

/**
 * DTO genérico de respuesta paginada para listas de recursos.
 *
 * <p>Envuelve una página de resultados junto con los metadatos de paginación
 * necesarios para que el cliente navegue entre páginas. Se utiliza como tipo
 * de retorno del endpoint {@code GET /api/v1/guias}.</p>
 *
 * <p>Ejemplo de uso con listas de guías:</p>
 * <pre>
 * PaginatedResponseDTO&lt;GuiaListItemDTO&gt; response = new PaginatedResponseDTO&lt;&gt;(
 *     items, totalElements, totalPages, currentPage, pageSize
 * );
 * </pre>
 *
 * <p><b>Requisitos relacionados:</b> 8.6, 8.7</p>
 *
 * @param <T> tipo de los elementos contenidos en la página
 */
public class PaginatedResponseDTO<T> {

    /**
     * Lista de elementos de la página actual.
     */
    private List<T> content;

    /**
     * Número total de elementos que coinciden con los filtros aplicados,
     * sin considerar la paginación.
     */
    private long totalElements;

    /**
     * Número total de páginas disponibles, calculado como
     * {@code ceil(totalElements / pageSize)}.
     */
    private int totalPages;

    /**
     * Índice de la página actual (base 0).
     */
    private int currentPage;

    /**
     * Tamaño máximo de elementos por página solicitado.
     */
    private int pageSize;

    // --- Constructores ---

    /** Constructor por defecto requerido por Jackson. */
    public PaginatedResponseDTO() {
    }

    /**
     * Constructor con todos los campos de paginación.
     *
     * @param content       lista de elementos de la página actual
     * @param totalElements número total de elementos que coinciden con los filtros
     * @param totalPages    número total de páginas disponibles
     * @param currentPage   índice de la página actual (base 0)
     * @param pageSize      tamaño máximo de la página solicitado
     */
    public PaginatedResponseDTO(List<T> content, long totalElements, int totalPages,
                                 int currentPage, int pageSize) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    // --- Getters y Setters ---

    /**
     * Devuelve la lista de elementos de la página actual.
     *
     * @return lista de elementos; puede ser vacía pero no {@code null}
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * Establece la lista de elementos de la página actual.
     *
     * @param content lista de elementos
     */
    public void setContent(List<T> content) {
        this.content = content;
    }

    /**
     * Devuelve el número total de elementos que coinciden con los filtros.
     *
     * @return totalElements
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * Establece el número total de elementos.
     *
     * @param totalElements número total de elementos sin paginar
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * Devuelve el número total de páginas disponibles.
     *
     * @return totalPages calculado como {@code ceil(totalElements / pageSize)}
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Establece el número total de páginas.
     *
     * @param totalPages número de páginas disponibles
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Devuelve el índice de la página actual (base 0).
     *
     * @return currentPage
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Establece el índice de la página actual.
     *
     * @param currentPage índice de página en base 0
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Devuelve el tamaño máximo de la página solicitado.
     *
     * @return pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Establece el tamaño máximo de la página.
     *
     * @param pageSize número máximo de elementos por página (1–100)
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "PaginatedResponseDTO{" +
                "content=" + content +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                '}';
    }
}
