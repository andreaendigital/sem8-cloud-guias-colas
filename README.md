# Sistema de Gestión de Guías de Despacho

Microservicio REST construido con **Spring Boot 3 / Java 17** para gestionar el ciclo de vida de Guías de Despacho: creación, generación asíncrona de PDF, almacenamiento en Amazon S3, descarga con JWT, actualización y eliminación lógica.

---

## Arquitectura General

```
Cliente HTTP
    │
    ▼
GuiaController  (/api/v1/guias)
    │
    ├── GuiaServiceImpl          ← lógica de negocio
    │       ├── GuiaRepository   ← JPA / H2 (local) o MySQL/PostgreSQL (prod)
    │       ├── PdfGeneratorServiceImpl  ← PDFBox + ZXing QR (async)
    │       └── S3StorageServiceImpl     ← AWS SDK v2 / LocalStack
    │
    └── JwtAuthFilter + JwtUtil  ← seguridad JWT HS256
```

Flujo principal: `POST /api/v1/guias` → persiste en DB (estado `BORRADOR`) → genera PDF en hilo separado → sube a S3 → estado `SUBIDA`.

---

## Estructura de Directorios

```
src/main/java/com/transportista/guias/
├── config/          AsyncConfig, AwsS3Config, JwtSecurityConfig, RetryConfig, SwaggerConfig
├── controller/      GuiaController
├── dto/             CrearGuiaRequestDTO, GuiaResponseDTO, PaginatedResponseDTO, ...
├── exception/       GuiaNotFoundException, GlobalExceptionHandler, ...
├── model/           Guia (entidad JPA), EstadoGuia (enum)
├── repository/      GuiaRepository
├── security/        JwtUtil, JwtAuthFilter
└── service/         GuiaService, GuiaServiceImpl, S3StorageService, PdfGeneratorService, ...
```

---

## Requisitos Previos

| Herramienta | Versión mínima |
|-------------|---------------|
| Java        | 17            |
| Maven       | 3.8+          |
| Docker      | 24+           |
| AWS CLI     | v2            |
| Git         | 2.x           |

---

## Ejecución en Local

### Opción 1 — Docker Compose (recomendada)

```bash
# Levanta la app + LocalStack (S3 simulado) + H2 en memoria
docker-compose up --build
```

La API queda disponible en `http://localhost:8080`.  
Documentación Swagger: `http://localhost:8080/swagger-ui/index.html`

### Opción 2 — Maven directo

```bash
# 1. Levantar LocalStack
docker run -d -p 4566:4566 localstack/localstack:3.0

# 2. Crear bucket de prueba
aws --endpoint-url=http://localhost:4566 s3 mb s3://guias-local

# 3. Ejecutar la app (credenciales fake para LocalStack)
set AWS_ACCESS_KEY_ID=test
set AWS_SECRET_ACCESS_KEY=test
mvn spring-boot:run
```

---

## Configuración de AWS (producción)

1. **Bucket S3**: `aws s3 mb s3://guias-despacho-prod --region us-east-1`
2. **EFS**: crear sistema de archivos EFS en la consola AWS y montarlo en EC2:
   ```bash
   sudo mount -t efs <efs-id>:/ /mnt/efs
   ```
3. **Usuario IAM mínimo**: política con permisos `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` sobre el bucket.
4. **EC2**: instancia `t2.micro` (capa gratuita), Amazon Linux 2, Docker instalado:
   ```bash
   sudo yum install docker -y && sudo service docker start
   ```

---

## Variables de Entorno

| Variable               | Descripción                          | Ejemplo                          | Obligatoria |
|------------------------|--------------------------------------|----------------------------------|-------------|
| `DB_URL`               | URL JDBC de la base de datos         | `jdbc:mysql://host:3306/guiasdb` | Sí          |
| `DB_USERNAME`          | Usuario de base de datos             | `guias_user`                     | Sí          |
| `DB_PASSWORD`          | Contraseña de base de datos          | `s3cret`                         | Sí          |
| `JWT_SECRET`           | Clave HS256 para JWT (≥ 32 chars)    | `mi-clave-secreta-de-32-chars!!` | Sí          |
| `AWS_ACCESS_KEY_ID`    | Clave de acceso AWS                  | `AKIAIOSFODNN7EXAMPLE`           | Sí (prod)   |
| `AWS_SECRET_ACCESS_KEY`| Clave secreta AWS                    | `wJalrXUtnFEMI/K7MDENG/...`      | Sí (prod)   |
| `AWS_REGION`           | Región AWS                           | `us-east-1`                      | Sí          |
| `S3_BUCKET_NAME`       | Nombre del bucket S3                 | `guias-despacho-prod`            | Sí          |
| `EFS_MOUNT_PATH`       | Ruta del directorio EFS montado      | `/mnt/efs/guias`                 | Sí          |
| `AWS_ENDPOINT_OVERRIDE`| Endpoint alternativo (LocalStack)    | `http://localhost:4566`          | No          |
| `PDF_ASYNC_POOL_SIZE`  | Tamaño thread pool generación PDF    | `4`                              | No          |

---

## Pipeline CI/CD

### Configurar secretos en GitHub

En tu repositorio: **Settings → Secrets and variables → Actions → New repository secret**.

Secretos necesarios: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `S3_BUCKET_NAME`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.

### Flujo automático

```
git push → main
    │
    ▼
Job 1: test-build-push
    ├── mvn test          (pruebas unitarias + propiedad jqwik)
    ├── mvn package       (genera JAR)
    └── docker build + push → Docker Hub (:latest + :SHA)
    │
    ▼
Job 2: deploy-ec2
    ├── SSH a EC2
    ├── docker pull
    ├── docker stop/rm contenedor anterior
    ├── docker run (con todas las variables de entorno)
    └── curl /actuator/health  (verificación de arranque)
```

---

## Estimación de Costos AWS (entorno de pruebas)

| Servicio           | Configuración                         | Costo estimado/mes |
|--------------------|---------------------------------------|--------------------|
| EC2 t2.micro       | 750 h/mes (capa gratuita primer año)  | $0                 |
| S3 Standard        | < 5 GB almacenados                    | $0 (capa gratuita) |
| RDS db.t3.micro    | 750 h/mes (capa gratuita primer año)  | $0                 |
| EFS Infrequent Access | < 1 GB (limpieza tras subida S3)   | ~$0.03             |
| Transferencia datos| Mínima con URLs pre-firmadas S3       | ~$0.09             |
| **Total estimado** |                                       | **< $1 / mes**     |

> Usando LocalStack en desarrollo local, el consumo de créditos AWS queda restringido al ambiente de producción en EC2.

---

## Endpoints Disponibles

| Método   | Ruta                              | Descripción              | Auth       |
|----------|-----------------------------------|--------------------------|------------|
| `POST`   | `/api/v1/guias`                   | Crear guía               | No         |
| `POST`   | `/api/v1/guias/{guiaId}/upload`   | Subida manual a S3       | Bearer JWT |
| `GET`    | `/api/v1/guias/{guiaId}/download` | Descargar PDF (redirect) | Bearer JWT |
| `PUT`    | `/api/v1/guias/{guiaId}`          | Actualizar guía          | Bearer JWT |
| `DELETE` | `/api/v1/guias/{guiaId}`          | Eliminar guía            | JWT ADMIN  |
| `GET`    | `/api/v1/guias`                   | Consultar paginado       | Bearer JWT |
| `GET`    | `/actuator/health`                | Health check             | No         |
