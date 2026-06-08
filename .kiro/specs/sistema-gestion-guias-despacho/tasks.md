# Implementation Plan: Sistema de Gestión de Guías de Despacho

## Overview

Implementación incremental del microservicio Spring Boot (Java 17) para gestión del ciclo de vida de Guías de Despacho. El plan cubre: estructura del proyecto Maven, entidad JPA y repositorio, capa de servicio con lógica de negocio, endpoints REST, seguridad JWT, generación asíncrona de PDF con PDFBox, integración con Amazon S3, pipeline CI/CD con GitHub Actions y documentación. Las pruebas basadas en propiedades (jqwik) validan las invariantes críticas definidas en el diseño.

---

## Tasks

- [x] 1. Estructura del proyecto y configuración base
  - Crear proyecto Maven con Spring Boot 3.x, Java 17 y las dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-retry`, `aws-java-sdk-v2` (s3 + s3-presigner), `pdfbox`, `zxing` (QR), `springdoc-openapi`, `jqwik`, `h2` (test), `testcontainers-localstack`.
  - Crear estructura de paquetes: `config/`, `controller/`, `dto/`, `exception/`, `model/`, `repository/`, `security/`, `service/`.
  - Crear `application.properties` con placeholders para todas las variables de entorno descritas en el diseño (`DB_URL`, `JWT_SECRET`, `S3_BUCKET_NAME`, `EFS_MOUNT_PATH`, `AWS_REGION`, `PDF_ASYNC_POOL_SIZE`).
  - Crear `AsyncConfig.java` con `ThreadPoolTaskExecutor` para el bean `pdfExecutor`.
  - Crear `SwaggerConfig.java` con configuración OpenAPI / Springdoc.
  - _Requirements: 2.5, 9.1, 11.6_

- [x] 2. Modelo de datos, entidad JPA y repositorio
  - [x] 2.1 Crear enum `EstadoGuia` con valores `BORRADOR`, `GENERADA`, `SUBIDA`, `ERROR_SUBIDA`, `ELIMINADA`
    - Archivo: `model/EstadoGuia.java`
    - _Requirements: 3.1, 7.2_

  - [x] 2.2 Crear entidad `Guia` con todos los campos del diseño (`guiaId` UUID auto-generado, `transportistaId`, `fechaEnvio`, `destinatario`, `direccionDestino`, `pesoKg`, `descripcionCarga`, `observaciones`, `estado`, `urlS3`, `fechaCreacion`, `fechaActualizacion`, `eliminado`)
    - Archivo: `model/Guia.java`
    - Usar `@GeneratedValue(strategy = GenerationType.UUID)`, `@Enumerated(EnumType.STRING)`, `@PrePersist` para `fechaCreacion`.
    - Incluir script SQL de inicialización `src/main/resources/schema.sql` con el DDL del diseño (índice en `transportista_id, fecha_envio`).
    - _Requirements: 3.3, 3.6, 6.6_

  - [x] 2.3 Crear `GuiaRepository` extendiendo `JpaRepository<Guia, UUID>`
    - Archivo: `repository/GuiaRepository.java`
    - Definir método de consulta: `findByTransportistaIdAndFechaEnvioBetweenAndEliminadoFalse(String transportistaId, LocalDate inicio, LocalDate fin, Pageable pageable)`.
    - _Requirements: 8.1, 8.6_

  - [ ]* 2.4 Escribir prueba de propiedad: round-trip de serialización de GuiaResponseDTO
    - **Property 9: Round-trip de serialización de GuiaResponseDTO**
    - **Validates: Requirements 3.1, 3.6, 8.2**
    - Clase de prueba: `GuiaResponseDTORoundTripPropertyTest`
    - Usar `@Property(tries = 100)` con `@ForAll` sobre instancias aleatorias de `Guia` válidas.
    - Serializar a JSON con Jackson, deserializar y comparar todos los campos.

- [x] 3. DTOs de request, response y excepciones de dominio
  - [x] 3.1 Crear DTOs: `CrearGuiaRequestDTO`, `ActualizarGuiaRequestDTO`, `GuiaResponseDTO`, `GuiaListItemDTO`, `PaginatedResponseDTO<T>`, `ErrorResponseDTO`
    - Aplicar anotaciones de validación Bean Validation (`@NotBlank`, `@NotNull`, `@Pattern`, `@Size`, `@DecimalMin`) sobre los campos de `CrearGuiaRequestDTO` y `ActualizarGuiaRequestDTO`.
    - `ActualizarGuiaRequestDTO` debe rechazar campos no modificables (usar `@JsonIgnoreProperties(ignoreUnknown = false)` o validación explícita en el servicio).
    - _Requirements: 3.2, 3.4, 3.5, 6.2_

  - [x] 3.2 Crear excepciones de dominio: `GuiaNotFoundException`, `GuiaYaEliminadaException`, `GuiaNoDisponibleException`, `S3UploadException`
    - Archivo: paquete `exception/`
    - _Requirements: 4.3, 5.5, 7.5_

  - [x] 3.3 Crear `GlobalExceptionHandler` con `@RestControllerAdvice`
    - Manejar: `MethodArgumentNotValidException` → HTTP 400 (lista de campos), `GuiaNotFoundException` → HTTP 404, `GuiaYaEliminadaException` → HTTP 409, `GuiaNoDisponibleException` → HTTP 409, `S3UploadException` → HTTP 502, `Exception` genérica → HTTP 500.
    - La respuesta de error debe usar `ErrorResponseDTO` con al menos los campos `status` y `message`.
    - _Requirements: 3.2, 2.4, 5.7_

- [x] 4. Checkpoint — Estructura base
  - Asegurarse de que el proyecto compila con `mvn compile` sin errores. Verificar que todas las clases de modelo, repositorio, DTOs y excepciones existen. Consultar al usuario si hay dudas.

- [x] 5. Seguridad JWT
  - [x] 5.1 Crear `JwtUtil.java` con métodos: `validateToken(String token)`, `extractSubject(String token)`, `extractRoles(String token)`, `extractTransportistasPermitidos(String token)`
    - Usar `io.jsonwebtoken` (JJWT) o `com.nimbusds` para parsear JWT HS256.
    - Leer `JWT_SECRET` desde `application.properties`.
    - _Requirements: 5.1, 5.3_

  - [x] 5.2 Crear `JwtAuthFilter` extendiendo `OncePerRequestFilter`
    - Interceptar todas las rutas excepto `POST /api/v1/guias` y `GET /actuator/health`.
    - Cargar `SecurityContext` con roles (`ROLE_ADMIN`, `ROLE_OPERADOR`) y claim `transportistasPermitidos`.
    - Devolver HTTP 401 si el token está ausente, expirado o con firma inválida.
    - _Requirements: 5.1, 5.3_

  - [x] 5.3 Crear `JwtSecurityConfig.java` con cadena de filtros Spring Security
    - Registrar `JwtAuthFilter` en la cadena de filtros.
    - Configurar autorización: `DELETE /api/v1/guias/**` requiere `ROLE_ADMIN`.
    - _Requirements: 5.3, 7.7_

  - [ ]* 5.4 Escribir pruebas unitarias para `JwtUtil` y `JwtAuthFilter`
    - Casos: token válido, token expirado, firma inválida, token ausente.
    - Usar ejemplos concretos con `@Test` de JUnit 5 y Mockito.
    - _Requirements: 5.1, 5.3_

- [x] 6. Servicio S3 y generación de PDF
  - [x] 6.1 Crear `AwsS3Config.java` con bean `S3Client` y `S3Presigner` de AWS SDK v2
    - Leer `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` desde variables de entorno.
    - Soportar endpoint override para LocalStack (`AWS_ENDPOINT_OVERRIDE`).
    - _Requirements: 2.5, 2.6_

  - [x] 6.2 Implementar `S3StorageServiceImpl` con la interfaz `S3StorageService`
    - Método `uploadFile(Path localPath, String s3Key)`: subir objeto a S3 usando AWS SDK v2 `PutObjectRequest`.
    - Anotar con `@Retryable(value = {SdkClientException.class, S3Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))`.
    - Método `@Recover recoverUpload(...)`: actualizar estado a `ERROR_SUBIDA` y lanzar `S3UploadException`.
    - Método `deleteObject(String s3Key)`: eliminar objeto de S3, no lanzar error si no existe (código `NoSuchKey`).
    - Método `generatePresignedUrl(String s3Key, Duration validity)`: generar URL pre-firmada con `S3Presigner`.
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 5.2, 7.3_

  - [ ]* 6.3 Escribir prueba de propiedad: construcción de ruta S3
    - **Property 2: La ruta S3 se construye correctamente para cualquier guía válida**
    - **Validates: Requirements 2.1**
    - Clase de prueba: `S3KeyBuilderPropertyTest`
    - Usar `@Property(tries = 200)` con `@ForAll` sobre fechas ISO 8601, `transportistaId` alfanuméricos y UUIDs v4.
    - Verificar que la cadena resultante tiene el patrón exacto `/{YYYYMM}/{transportistaId}/{guiaId}.pdf`.

  - [x] 6.4 Implementar `PdfGeneratorServiceImpl` con la interfaz `PdfGeneratorService`
    - Método `generarYSubir(Guia guia)` anotado con `@Async("pdfExecutor")`, retorna `CompletableFuture<Void>`.
    - Usar PDFBox para crear un `PDDocument` con un `PDPage`, escribir con `PDPageContentStream`: `guiaId`, `transportistaId`, `fechaEnvio`, `destinatario`, `direccionDestino`, `pesoKg`, `descripcionCarga`, `observaciones` (si presente).
    - Generar código QR con ZXing codificando la URL `/api/v1/guias/{guiaId}/download` e insertarlo en el PDF.
    - Guardar PDF en EFS: `{EFS_MOUNT_PATH}/{guiaId}.pdf`.
    - Llamar a `S3StorageService.uploadFile(...)` con la ruta `/{YYYYMM}/{transportistaId}/{guiaId}.pdf`.
    - Actualizar estado de la guía a `SUBIDA` y `urlS3` en la base de datos.
    - Eliminar archivo de EFS tras subida exitosa.
    - Manejar errores según la estrategia del diseño (log ERROR/WARN, estados `ERROR_SUBIDA`).
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ]* 6.5 Escribir prueba de propiedad: el PDF generado contiene todos los campos requeridos
    - **Property 10: El PDF generado contiene todos los campos requeridos de la guía**
    - **Validates: Requirements 11.1, 11.5**
    - Clase de prueba: `PdfGeneratorPropertyTest`
    - Usar `@Property(tries = 100)` con `@ForAll` sobre instancias de `Guia` válidas con `observaciones` presentes y ausentes.
    - Extraer texto del PDF generado con PDFBox `PDFTextStripper` y verificar presencia de campos obligatorios.

- [x] 7. Checkpoint — Servicios de infraestructura
  - Asegurar que `mvn test` pasa para los módulos de seguridad, S3 y PDF. Verificar que `S3StorageServiceImpl` compila con Spring Retry configurado. Consultar al usuario si hay dudas.

- [x] 8. Lógica de negocio: GuiaService
  - [x] 8.1 Implementar `GuiaServiceImpl.crearGuia(CrearGuiaRequestDTO dto)`
    - Persistir nueva `Guia` con estado `BORRADOR` y `fechaCreacion` en UTC.
    - Lanzar `DataIntegrityViolationException` → HTTP 500 si falla persistencia.
    - Invocar `PdfGeneratorService.generarYSubir(guia)` de forma asíncrona (no bloquear respuesta).
    - Mapear a `GuiaResponseDTO` y retornar.
    - _Requirements: 3.1, 3.6, 3.7, 3.8_

  - [ ]* 8.2 Escribir prueba de propiedad: creación con datos válidos siempre produce BORRADOR y UUID único
    - **Property 1: Creación con datos válidos siempre produce guía con estado BORRADOR y guiaId UUID v4 único**
    - **Validates: Requirements 3.1, 3.3, 3.6**
    - Clase de prueba: `GuiaCreacionPropertyTest`
    - Usar `@Property(tries = 100)` con `@ForAll` sobre combinaciones válidas de `transportistaId`, `fechaEnvio`, `destinatario`, `direccionDestino`.
    - Verificar estado `BORRADOR`, formato UUID v4 del `guiaId` y unicidad entre pares de guías.

  - [ ]* 8.3 Escribir prueba de propiedad: campos obligatorios faltantes siempre producen HTTP 400
    - **Property 3: Campos obligatorios faltantes siempre producen HTTP 400 con lista de campos**
    - **Validates: Requirements 3.2**
    - Clase de prueba: `GuiaValidacionObligatoriosPropertyTest`
    - Usar `@Property(tries = 200)` con `@ForAll` sobre payloads con al menos un campo obligatorio ausente.
    - Verificar HTTP 400 y que el mensaje enumera todos los campos faltantes.

  - [ ]* 8.4 Escribir prueba de propiedad: validación de formato y restricciones de campos individuales
    - **Property 4: Validación de formato y restricciones de campos individuales**
    - **Validates: Requirements 3.4, 3.5**
    - Clase de prueba: `GuiaCamposFormatoPropertyTest`
    - Usar `@Property(tries = 200)` con `@ForAll` sobre strings con `fechaEnvio` inválida y `transportistaId` con caracteres no alfanuméricos o longitud > 50.
    - Verificar HTTP 400 en todos los casos.

  - [x] 8.5 Implementar `GuiaServiceImpl.uploadGuia(UUID guiaId)`
    - Verificar existencia de la guía → 404 si no existe.
    - Verificar estado: si `SUBIDA` → 409; si `ELIMINADA` → 409.
    - Verificar existencia del PDF en EFS → 404 si no existe.
    - Invocar `S3StorageService.uploadFile(...)` y actualizar estado a `SUBIDA`.
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 8.6 Implementar `GuiaServiceImpl.downloadGuia(UUID guiaId, String jwtToken, HttpServletResponse response)`
    - Verificar existencia (incluye lógica `eliminado=true` → 404).
    - Verificar estado `SUBIDA` → 409 si no está subida.
    - Verificar permiso del token sobre el `transportistaId` → 403 si no tiene permiso.
    - Generar URL pre-firmada con vigencia de 15 minutos y hacer redirect HTTP 302.
    - _Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 5.7_

  - [x] 8.7 Implementar `GuiaServiceImpl.actualizarGuia(UUID guiaId, ActualizarGuiaRequestDTO dto)`
    - Verificar existencia → 404; estado `ELIMINADA` → 409.
    - Rechazar campos no permitidos → 400.
    - Actualizar solo los campos presentes (`destinatario`, `direccionDestino`, `pesoKg`, `descripcionCarga`, `observaciones`).
    - Si estado era `SUBIDA`, cambiar a `BORRADOR` e iniciar regeneración asíncrona del PDF.
    - Actualizar `fechaActualizacion` en UTC.
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [x]* 8.8 Escribir prueba de propiedad: campos no permitidos en actualización producen HTTP 400
    - **Property 5: Campos no permitidos en actualización siempre producen HTTP 400**
    - **Validates: Requirements 6.2**
    - Clase de prueba: `GuiaActualizacionCamposPropertyTest`
    - Usar `@Property(tries = 200)` con `@ForAll` sobre payloads PUT con al menos un campo fuera del conjunto permitido.
    - Verificar HTTP 400 independientemente de si los campos válidos son correctos.

  - [x] 8.9 Implementar `GuiaServiceImpl.eliminarGuia(UUID guiaId, String jwtToken)`
    - Verificar existencia → 404; estado `ELIMINADA` → 409; rol `ADMIN` del token → 403.
    - Marcar `eliminado=true` y estado `ELIMINADA` (soft delete).
    - Intentar `S3StorageService.deleteObject(...)` (ignorar error si objeto no existe, log INFO).
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [x] 8.10 Implementar `GuiaServiceImpl.consultarGuias(String transportistaId, String fecha, int page, int size)`
    - Parsear `fecha` (formato `YYYYMM`) al rango de fechas del mes → 400 si formato inválido.
    - Validar parámetros de paginación (`page ≥ 0`, `1 ≤ size ≤ 100`) → 400 si fuera de rango.
    - Consultar repositorio excluyendo `eliminado=true`, mapear a `GuiaListItemDTO`.
    - Retornar `PaginatedResponseDTO` con `totalElements`, `totalPages`, `currentPage`, `pageSize`.
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [x]* 8.11 Escribir prueba de propiedad: paginación respeta invariantes matemáticos y excluye eliminadas
    - **Property 6: Consulta paginada respeta invariantes matemáticos y excluye eliminadas**
    - **Validates: Requirements 8.1, 8.6, 8.7**
    - Clase de prueba: `GuiaPaginacionPropertyTest`
    - Usar `@Property(tries = 100)` con `@ForAll` sobre combinaciones válidas de `page` (0 ≤ page) y `size` (1 ≤ size ≤ 100) con datos semilla en H2.
    - Verificar: `content.size() ≤ size`, `totalPages = ceil(totalElements / size)`, ningún elemento con estado `ELIMINADA`.

  - [x]* 8.12 Escribir prueba de propiedad: parámetros de paginación fuera de rango producen HTTP 400
    - **Property 7: Parámetros de paginación fuera de rango siempre producen HTTP 400**
    - **Validates: Requirements 8.8, 8.9**
    - Clase de prueba: `GuiaPaginacionInvalidaPropertyTest`
    - Usar `@Property(tries = 200)` con `@ForAll` donde `size > 100`, `page < 0` o `size < 1`.
    - Verificar HTTP 400 en todos los casos.

  - [x]* 8.13 Escribir prueba de propiedad: parámetros obligatorios de consulta ausentes producen HTTP 400
    - **Property 8: Validación de parámetros obligatorios de consulta**
    - **Validates: Requirements 8.4, 8.5**
    - Clase de prueba: `GuiaConsultaParametrosPropertyTest`
    - Usar `@Property(tries = 200)` con solicitudes sin `transportistaId` y con valores de `fecha` en formato inválido (strings vacíos, letras, meses fuera de rango).
    - Verificar HTTP 400 en todos los casos.

- [ ] 9. Controlador REST
  - [x] 9.1 Implementar `GuiaController` con todos los endpoints definidos en el diseño
    - `POST /api/v1/guias` → `crearGuia` → HTTP 201.
    - `POST /api/v1/guias/{guiaId}/upload` → `uploadGuia` → HTTP 200.
    - `GET /api/v1/guias/{guiaId}/download` → `downloadGuia` → HTTP 302.
    - `PUT /api/v1/guias/{guiaId}` → `actualizarGuia` → HTTP 200.
    - `DELETE /api/v1/guias/{guiaId}` → `eliminarGuia` → HTTP 200.
    - `GET /api/v1/guias` → `consultarGuias` → HTTP 200.
    - Anotar con `@RestController`, `@RequestMapping("/api/v1/guias")`, `@Validated`.
    - Incluir Javadoc en todos los métodos públicos.
    - _Requirements: 3.1, 4.1, 5.2, 6.1, 7.1, 8.1_

  - [ ]* 9.2 Escribir pruebas unitarias MockMvc para `GuiaController`
    - Probar happy path de cada endpoint con Mockito (`@WebMvcTest`).
    - Probar respuestas 400, 401, 403, 404, 409, 502 con ejemplos concretos.
    - _Requirements: 3.1, 4.1, 5.1, 6.1, 7.1, 8.1_

- [x] 10. Checkpoint — API REST completa
  - Ejecutar `mvn test` y verificar que todas las pruebas unitarias y de propiedad pasan. Verificar que el contexto Spring arranca con `@SpringBootTest` usando H2 y configuración mínima. Consultar al usuario si hay dudas.

- [ ] 11. Pruebas de integración con LocalStack y Testcontainers
  - [~] 11.1 Configurar `Testcontainers` con la imagen `localstack/localstack:3.0` y servicio S3
    - Crear clase base `AbstractIntegrationTest` con `@Testcontainers` que levante LocalStack y configure `S3Client` apuntando a `http://localhost:4566`.
    - Crear bucket S3 de prueba en el `@BeforeAll`.
    - _Requirements: 2.1, 2.2_

  - [~] 11.2 Escribir prueba de integración: flujo completo upload → presigned URL → descarga
    - Subir un PDF de prueba vía `S3StorageService.uploadFile(...)`, generar URL pre-firmada y verificar que la URL es accesible (HTTP 200).
    - Verificar que `deleteObject(...)` no lanza error cuando el objeto existe ni cuando no existe.
    - _Requirements: 2.1, 2.2, 5.2_

  - [ ]* 11.3 Escribir prueba de integración: ciclo de vida completo de una guía via API REST
    - Usando `@SpringBootTest` + MockMvc + H2 + LocalStack:
      - POST crear guía → verificar HTTP 201 y estado `BORRADOR`.
      - Esperar generación asíncrona → verificar estado `SUBIDA`.
      - GET consultar guía → verificar presencia en lista.
      - PUT actualizar guía → verificar HTTP 200 y `fechaActualizacion`.
      - DELETE eliminar guía → verificar HTTP 200 y soft delete.
    - _Requirements: 3.1, 6.1, 7.1, 8.1_

- [ ] 12. Pipeline CI/CD y configuración de despliegue
  - [x] 12.1 Crear `Dockerfile` en la raíz del proyecto
    - Usar imagen base `eclipse-temurin:17-jre-alpine`.
    - Copiar JAR generado por Maven, exponer puerto 8080, definir `ENTRYPOINT`.
    - _Requirements: 9.1_

  - [x] 12.2 Crear `docker-compose.yml` para desarrollo local con LocalStack
    - Servicio `app` con todas las variables de entorno de desarrollo y `AWS_ENDPOINT_OVERRIDE=http://localstack:4566`.
    - Servicio `localstack` con imagen `localstack/localstack:3.0` y servicio S3 habilitado.
    - _Requirements: 9.1, 12.3_

  - [x] 12.3 Crear `.github/workflows/ci-cd.yml` con el pipeline definido en el diseño
    - Job `test-build-push`: checkout, setup Java 17, `mvn test`, `docker/build-push-action` con etiquetas `latest` y SHA corto.
    - Job `deploy-ec2` (depends on `test-build-push`): `appleboy/ssh-action` con script de despliegue (`docker pull`, `docker stop`, `docker rm`, `docker run` con todas las variables de entorno), verificación `curl /actuator/health`.
    - Usar secretos: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `S3_BUCKET_NAME`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_

- [ ] 13. Documentación
  - [x] 13.1 Crear `README.md` en la raíz del repositorio con las secciones requeridas
    - Sección "Arquitectura General" con descripción del microservicio y sus componentes.
    - Sección "Estructura de Directorios" con árbol de paquetes Java y propósito de cada módulo.
    - Sección "Requisitos Previos": Java 17, Maven 3.8+, Docker 24+, AWS CLI v2, Git 2.x.
    - Sección "Ejecución en Local" con comandos exactos `docker-compose up`.
    - Sección "Configuración de AWS": crear bucket S3, montar EFS en EC2, crear usuario IAM mínimo, lanzar EC2 t2.micro.
    - Sección "Variables de Entorno" con tabla completa de variables, ejemplos y obligatoriedad.
    - Sección "Pipeline CI/CD" con pasos para configurar secretos en GitHub y descripción del flujo.
    - Sección "Estimación de Costos AWS" con tabla de servicios y costos estimados < $1/mes.
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.8_

  - [x] 13.2 Agregar Javadoc a todas las clases de servicio, repositorio y controlador
    - Añadir `/** ... */` en `GuiaService`, `GuiaServiceImpl`, `GuiaRepository`, `GuiaController`, `S3StorageService`, `S3StorageServiceImpl`, `PdfGeneratorService`, `PdfGeneratorServiceImpl`, `JwtUtil`, `JwtAuthFilter`.
    - Documentar responsabilidad de la clase y contrato de cada método público.
    - _Requirements: 12.7_

- [~] 14. Checkpoint final — Todos los tests y compilación
  - Ejecutar `mvn verify` para correr pruebas unitarias, de propiedad e integración.
  - Verificar que la imagen Docker se construye exitosamente con `docker build`.
  - Verificar que `GET /actuator/health` devuelve HTTP 200 al arrancar con configuración local.
  - Consultar al usuario si hay dudas antes de concluir.

---

## Notes

- Las sub-tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido, pero se recomienda implementarlas para validar las invariantes del diseño.
- Cada tarea referencia requisitos específicos para trazabilidad completa.
- El lenguaje de implementación es **Java 17** con **Spring Boot 3.x**.
- Las pruebas de propiedad usan **jqwik** con mínimo 100 iteraciones por propiedad.
- Las pruebas de integración requieren **Docker** disponible en el entorno de ejecución para Testcontainers/LocalStack.
- Los checkpoints (tareas 4, 7, 10, 14) no generan código pero garantizan validación incremental.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1", "3.2"] },
    { "id": 1, "tasks": ["2.2", "3.1", "5.1"] },
    { "id": 2, "tasks": ["2.3", "3.3", "5.2", "6.1"] },
    { "id": 3, "tasks": ["2.4", "5.3", "6.2", "8.1"] },
    { "id": 4, "tasks": ["5.4", "6.3", "6.4", "8.2", "8.3", "8.4"] },
    { "id": 5, "tasks": ["6.5", "8.5", "8.6", "8.7", "8.9", "8.10"] },
    { "id": 6, "tasks": ["8.8", "8.11", "8.12", "8.13", "9.1"] },
    { "id": 7, "tasks": ["9.2", "11.1", "13.1", "13.2"] },
    { "id": 8, "tasks": ["11.2", "12.1", "12.2"] },
    { "id": 9, "tasks": ["11.3", "12.3"] }
  ]
}
```
