package com.transportista.guias.service;

import com.transportista.guias.dto.ActualizarGuiaRequestDTO;
import com.transportista.guias.dto.CrearGuiaRequestDTO;
import com.transportista.guias.dto.GuiaListItemDTO;
import com.transportista.guias.dto.GuiaResponseDTO;
import com.transportista.guias.dto.PaginatedResponseDTO;
import com.transportista.guias.exception.GuiaNoDisponibleException;
import com.transportista.guias.exception.GuiaNotFoundException;
import com.transportista.guias.exception.GuiaYaEliminadaException;
import com.transportista.guias.exception.S3UploadException;
import com.transportista.guias.model.EstadoGuia;
import com.transportista.guias.model.Guia;
import com.transportista.guias.repository.GuiaRepository;
import com.transportista.guias.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación concreta del contrato de negocio {@link GuiaService}.
 *
 * <p>Gestiona el ciclo de vida completo de las Guías de Despacho: creación,
 * subida manual a S3, descarga con control de acceso JWT, actualización,
 * eliminación lógica y consulta paginada.</p>
 *
 * <p>La dependencia sobre {@link PdfGeneratorService} se inyecta con
 * {@code @Lazy} para evitar dependencias circulares entre los beans de Spring
 * cuando {@code PdfGeneratorServiceImpl} a su vez depende de este servicio u
 * otros beans del contexto.</p>
 *
 * <p><b>Requisitos relacionados:</b> 3.1, 3.6, 3.7, 3.8</p>
 */
@Service
public class GuiaServiceImpl implements GuiaService {

    /** Repositorio JPA para operaciones CRUD sobre la entidad {@link Guia}. */
    private final GuiaRepository guiaRepository;

    /**
     * Servicio de generación asíncrona de PDFs. Inyectado con {@code @Lazy}
     * para prevenir dependencias circulares en el contexto de Spring.
     */
    private final PdfGeneratorService pdfGeneratorService;

    /** Servicio para operaciones con Amazon S3 (subida, eliminación, URLs pre-firmadas). */
    private final S3StorageService s3StorageService;

    /** Utilidad para parsear y validar tokens JWT (extracción de claims). */
    private final JwtUtil jwtUtil;

    /**
     * Ruta del punto de montaje de EFS donde se almacenan los PDFs generados
     * antes de su subida a S3. Configurada mediante la variable de entorno
     * {@code EFS_MOUNT_PATH}.
     */
    @Value("${efs.mount.path:/mnt/efs/guias}")
    private String efsMountPath;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructor con inyección de dependencias.
     *
     * @param guiaRepository      repositorio JPA de guías; no debe ser {@code null}.
     * @param pdfGeneratorService servicio de generación asíncrona de PDFs; inyectado
     *                            con {@code @Lazy} para evitar dependencias circulares.
     * @param s3StorageService    servicio de operaciones con Amazon S3.
     * @param jwtUtil             utilidad para parsear y validar tokens JWT.
     */
    public GuiaServiceImpl(GuiaRepository guiaRepository,
                           @Lazy PdfGeneratorService pdfGeneratorService,
                           S3StorageService s3StorageService,
                           JwtUtil jwtUtil) {
        this.guiaRepository = guiaRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.s3StorageService = s3StorageService;
        this.jwtUtil = jwtUtil;
    }

    // -------------------------------------------------------------------------
    // GuiaService implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Persiste la guía en base de datos con estado {@link EstadoGuia#BORRADOR} y
     * dispara de forma no bloqueante la generación del PDF mediante
     * {@link PdfGeneratorService#generarYSubir(Guia)}.</p>
     *
     * <p><b>Requisitos:</b> 3.1, 3.6</p>
     */
    @Override
    public GuiaResponseDTO crearGuia(CrearGuiaRequestDTO dto) {
        Guia guia = new Guia();
        guia.setTransportistaId(dto.getTransportistaId());
        guia.setFechaEnvio(dto.getFechaEnvio());
        guia.setDestinatario(dto.getDestinatario());
        guia.setDireccionDestino(dto.getDireccionDestino());
        guia.setPesoKg(dto.getPesoKg());
        guia.setDescripcionCarga(dto.getDescripcionCarga());
        guia.setObservaciones(dto.getObservaciones());
        guia.setEstado(EstadoGuia.BORRADOR);
        guia.setEliminado(false);

        Guia saved = guiaRepository.save(guia);

        // Async PDF generation — non-blocking
        pdfGeneratorService.generarYSubir(saved);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifica la existencia de la guía, su estado y la presencia del PDF en EFS,
     * luego sube el archivo a S3 y actualiza el estado a {@link EstadoGuia#SUBIDA}.</p>
     *
     * <p><b>Requisitos:</b> 4.1, 4.2, 4.3, 4.4, 4.5, 4.6</p>
     *
     * @throws GuiaNotFoundException      si la guía no existe (404) o el PDF no está en EFS (404).
     * @throws GuiaNoDisponibleException  si la guía ya está en estado SUBIDA (409)
     *                                   o está eliminada (409).
     */
    @Override
    public GuiaResponseDTO uploadGuia(UUID guiaId) {
        // 4.1 — Verificar existencia de la guía
        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException("Guía no encontrada"));

        // 4.2 — Verificar estado: SUBIDA → 409
        if (guia.getEstado() == EstadoGuia.SUBIDA) {
            throw new GuiaNoDisponibleException("La guía ya ha sido subida a S3");
        }

        // 4.3 — Verificar estado: ELIMINADA (por estado o por flag) → 409
        if (guia.getEstado() == EstadoGuia.ELIMINADA || guia.isEliminado()) {
            throw new GuiaNoDisponibleException("No es posible subir una guía eliminada");
        }

        // 4.4 — Verificar existencia del PDF en EFS
        Path efsPdfPath = Paths.get(efsMountPath, guiaId.toString() + ".pdf");
        if (!Files.exists(efsPdfPath)) {
            throw new GuiaNotFoundException("Archivo PDF no encontrado en almacenamiento temporal");
        }

        // 4.5 — Construir clave S3 e invocar S3StorageService.uploadFile(...)
        String yyyymm = guia.getFechaEnvio().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String s3Key = "/" + yyyymm + "/" + guia.getTransportistaId() + "/" + guiaId + ".pdf";

        s3StorageService.uploadFile(efsPdfPath, s3Key);

        // 4.6 — Actualizar estado a SUBIDA y persistir
        guia.setEstado(EstadoGuia.SUBIDA);
        guia.setUrlS3(s3Key);
        guia.setFechaActualizacion(Instant.now());
        Guia saved = guiaRepository.save(guia);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Valida existencia, estado {@link EstadoGuia#SUBIDA} y permiso del token sobre
     * el {@code transportistaId}. Si todo es correcto genera una URL pre-firmada S3
     * válida 15 minutos y hace redirect HTTP 302.</p>
     *
     * <p><b>Requisitos:</b> 5.1, 5.2, 5.4, 5.5, 5.6, 5.7</p>
     *
     * @throws GuiaNotFoundException      si la guía no existe o está eliminada (HTTP 404)
     * @throws GuiaNoDisponibleException  si la guía no está en estado SUBIDA (HTTP 409)
     * @throws AccessDeniedException      si el token no autoriza acceso al transportistaId (HTTP 403)
     * @throws S3UploadException          si falla la generación de la URL pre-firmada (HTTP 502)
     */
    @Override
    public void downloadGuia(UUID guiaId, String jwtToken, HttpServletResponse response) {
        // 5.5 — Verificar existencia (excluir eliminados)
        Guia guia = guiaRepository.findById(guiaId)
                .filter(g -> !g.isEliminado())
                .orElseThrow(() -> new GuiaNotFoundException("Guía no encontrada"));

        // 5.6 — Verificar estado SUBIDA
        if (guia.getEstado() != EstadoGuia.SUBIDA) {
            throw new GuiaNoDisponibleException("La guía no está disponible para descarga");
        }

        // 5.4 — Verificar permiso del token sobre el transportistaId
        List<String> permitidos = jwtUtil.extractTransportistasPermitidos(jwtToken);
        if (!permitidos.contains(guia.getTransportistaId())) {
            throw new AccessDeniedException("No tiene permisos para descargar esta guía");
        }

        // 5.2 — Generar URL pre-firmada y redirigir (HTTP 302)
        try {
            URL presignedUrl = s3StorageService.generatePresignedUrl(
                    guia.getUrlS3(), Duration.ofMinutes(15));
            response.setHeader("Location", presignedUrl.toString());
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        } catch (Exception e) {
            throw new S3UploadException("Error al generar enlace de descarga", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Busca la guía por {@code guiaId}; lanza {@link GuiaNotFoundException} (404) si no
     * existe. Lanza {@link GuiaYaEliminadaException} (409) si la guía está en estado
     * {@link EstadoGuia#ELIMINADA} o tiene el flag de soft-delete activo.</p>
     *
     * <p>Actualiza únicamente los campos presentes en el DTO (null → sin cambio).
     * Si la guía estaba en estado {@link EstadoGuia#SUBIDA}, la retrocede a
     * {@link EstadoGuia#BORRADOR} y dispara la regeneración asíncrona del PDF.
     * Establece {@code fechaActualizacion} al instante actual en UTC.</p>
     *
     * <p><b>Requisitos:</b> 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7</p>
     *
     * @param guiaId identificador único de la guía a actualizar
     * @param dto    campos a actualizar (solo los no-null se aplican)
     * @return {@link GuiaResponseDTO} con el estado actualizado de la guía
     * @throws GuiaNotFoundException      si la guía no existe (HTTP 404)
     * @throws GuiaYaEliminadaException   si la guía está eliminada (HTTP 409)
     */
    @Override
    public GuiaResponseDTO actualizarGuia(UUID guiaId, ActualizarGuiaRequestDTO dto) {
        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException("Guía no encontrada"));

        if (guia.getEstado() == EstadoGuia.ELIMINADA || guia.isEliminado()) {
            throw new GuiaYaEliminadaException("No es posible modificar una guía eliminada");
        }

        boolean wasSubida = guia.getEstado() == EstadoGuia.SUBIDA;

        if (dto.getDestinatario() != null) guia.setDestinatario(dto.getDestinatario());
        if (dto.getDireccionDestino() != null) guia.setDireccionDestino(dto.getDireccionDestino());
        if (dto.getPesoKg() != null) guia.setPesoKg(dto.getPesoKg());
        if (dto.getDescripcionCarga() != null) guia.setDescripcionCarga(dto.getDescripcionCarga());
        if (dto.getObservaciones() != null) guia.setObservaciones(dto.getObservaciones());

        if (wasSubida) {
            guia.setEstado(EstadoGuia.BORRADOR);
        }
        guia.setFechaActualizacion(Instant.now());

        Guia saved = guiaRepository.save(guia);

        if (wasSubida) {
            pdfGeneratorService.generarYSubir(saved);
        }

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifica rol ADMIN en el token, existencia de la guía y que no esté ya
     * eliminada. Aplica soft-delete (eliminado=true, estado ELIMINADA) y luego
     * intenta borrar el objeto S3 de forma no bloqueante (ignorar si no existe).</p>
     *
     * <p><b>Requisitos:</b> 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7</p>
     *
     * @throws AccessDeniedException     si el token no tiene rol ROLE_ADMIN (HTTP 403)
     * @throws GuiaNotFoundException     si la guía no existe (HTTP 404)
     * @throws GuiaYaEliminadaException  si la guía ya está eliminada (HTTP 409)
     */
    @Override
    public void eliminarGuia(UUID guiaId, String jwtToken) {
        // 7.7 — Verificar rol ADMIN
        List<String> roles = jwtUtil.extractRoles(jwtToken);
        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("No tiene permisos para eliminar guías");
        }

        // 7.5 — Verificar existencia
        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException("Guía no encontrada"));

        // 7.6 — Verificar no esté ya eliminada
        if (guia.getEstado() == EstadoGuia.ELIMINADA || guia.isEliminado()) {
            throw new GuiaYaEliminadaException("La guía ya ha sido eliminada");
        }

        // 7.2 — Soft delete
        guia.setEliminado(true);
        guia.setEstado(EstadoGuia.ELIMINADA);
        guia.setFechaActualizacion(Instant.now());
        guiaRepository.save(guia);

        // 7.3/7.4 — Intentar eliminar de S3 (ignorar si no existe; deleteObject lo maneja)
        if (guia.getUrlS3() != null) {
            s3StorageService.deleteObject(guia.getUrlS3());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Parsea el parámetro {@code fecha} (formato YYYYMM) al rango de fechas del mes,
     * valida los límites de paginación y consulta el repositorio excluyendo registros
     * eliminados. Retorna {@link PaginatedResponseDTO} con metadatos de paginación.</p>
     *
     * <p><b>Requisitos:</b> 8.1–8.9</p>
     *
     * @throws IllegalArgumentException si transportistaId es nulo/vacío, fecha tiene
     *         formato inválido o los parámetros de paginación están fuera de rango.
     */
    @Override
    public PaginatedResponseDTO<GuiaListItemDTO> consultarGuias(
            String transportistaId, String fecha, int page, int size) {

        // 8.4 — transportistaId obligatorio
        if (transportistaId == null || transportistaId.isBlank()) {
            throw new IllegalArgumentException("El parámetro transportistaId es obligatorio");
        }

        // 8.8 — size > 100
        if (size > 100) {
            throw new IllegalArgumentException("El parámetro size no puede superar el valor de 100");
        }

        // 8.9 — page < 0 o size < 1
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Los parámetros de paginación deben ser valores positivos");
        }

        // 8.5 — Parsear fecha YYYYMM
        if (fecha == null || !fecha.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "El formato del parámetro fecha debe ser YYYYMM con mes entre 01 y 12");
        }
        int year = Integer.parseInt(fecha.substring(0, 4));
        int month = Integer.parseInt(fecha.substring(4, 6));
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "El formato del parámetro fecha debe ser YYYYMM con mes entre 01 y 12");
        }
        LocalDate inicio = LocalDate.of(year, month, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        // 8.1 / 8.6 — Consultar repositorio (excluye eliminados)
        Pageable pageable = PageRequest.of(page, size);
        Page<Guia> pageResult = guiaRepository
                .findByTransportistaIdAndFechaEnvioBetweenAndEliminadoFalse(
                        transportistaId, inicio, fin, pageable);

        // 8.2 — Mapear a GuiaListItemDTO
        List<GuiaListItemDTO> items = pageResult.getContent().stream()
                .map(g -> new GuiaListItemDTO(
                        g.getGuiaId(),
                        g.getTransportistaId(),
                        g.getFechaEnvio(),
                        g.getEstado(),
                        g.getUrlS3(),
                        g.getFechaCreacion()))
                .collect(Collectors.toList());

        // 8.3 / 8.7 — Retornar con metadatos
        return new PaginatedResponseDTO<>(
                items,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                page,
                size);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Convierte una entidad {@link Guia} en su representación {@link GuiaResponseDTO}.
     *
     * <p>Mapea todos los campos de la entidad al DTO de respuesta, incluyendo
     * {@code urlS3} (puede ser {@code null} mientras la guía no esté en estado
     * {@code SUBIDA}) y las marcas de tiempo de creación y actualización.</p>
     *
     * @param guia entidad a convertir; no debe ser {@code null}.
     * @return {@link GuiaResponseDTO} con todos los campos mapeados.
     */
    private GuiaResponseDTO mapToResponseDTO(Guia guia) {
        GuiaResponseDTO dto = new GuiaResponseDTO();
        dto.setGuiaId(guia.getGuiaId());
        dto.setTransportistaId(guia.getTransportistaId());
        dto.setFechaEnvio(guia.getFechaEnvio());
        dto.setDestinatario(guia.getDestinatario());
        dto.setDireccionDestino(guia.getDireccionDestino());
        dto.setPesoKg(guia.getPesoKg());
        dto.setDescripcionCarga(guia.getDescripcionCarga());
        dto.setObservaciones(guia.getObservaciones());
        dto.setEstado(guia.getEstado());
        dto.setUrlS3(guia.getUrlS3());
        dto.setFechaCreacion(guia.getFechaCreacion());
        dto.setFechaActualizacion(guia.getFechaActualizacion());
        return dto;
    }
}
