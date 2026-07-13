# Requirements Document

## Introduction

Este documento describe los requisitos funcionales y no funcionales del **Sistema de Gestión de Pedidos y Generación de Guías de Despacho** para una empresa transportista. El sistema expone una API REST construida con **Spring Boot** que permite crear, consultar, actualizar, eliminar y descargar guías de despacho en formato PDF. Las guías se almacenan temporalmente en un sistema de archivos compartido (EFS) y se persisten de forma definitiva en **Amazon S3**, organizadas por fecha y transportista. El sistema se despliega automáticamente en una instancia **EC2** mediante imágenes Docker publicadas en **Docker Hub** a través de un pipeline de **GitHub Actions**.

El documento también incluye indicaciones pedagógicas sobre la estructura del código, cómo ejecutar el proyecto en local y cómo conectarlo a servicios AWS de forma eficiente, minimizando el uso de créditos (objetivo: menos de 30 créditos AWS).

---

## Glossary

- **Sistema**: El microservicio Spring Boot de gestión de guías de despacho.
- **Guía de Despacho**: Documento PDF que acredita el envío de un paquete o carga por parte de un transportista.
- **Transportista**: Entidad (empresa o persona) responsable del transporte de la carga identificada con un identificador único (`transportistaId`).
- **EFS (Elastic File System)**: Sistema de archivos en red de AWS montado en el microservicio para almacenamiento temporal de guías generadas.
- **S3 (Simple Storage Service)**: Servicio de almacenamiento de objetos de AWS utilizado para la persistencia definitiva de las guías.
- **Bucket S3**: Contenedor lógico en S3 donde se almacenan los objetos (guías PDF).
- **Ruta S3**: Ruta jerárquica dentro del bucket con formato `/{YYYYMM}/{transportistaId}/{guiaId}.pdf`.
- **API REST**: Interfaz de programación de aplicaciones basada en HTTP que sigue el estilo arquitectónico REST.
- **Endpoint**: URL específica de la API REST que recibe y responde a solicitudes HTTP.
- **Docker Hub**: Registro de imágenes de contenedores Docker utilizado para publicar la imagen del microservicio.
- **EC2**: Servicio de cómputo de AWS (Elastic Compute Cloud) donde se despliega el microservicio en producción.
- **GitHub Actions**: Plataforma de integración y entrega continua (CI/CD) integrada en GitHub.
- **JWT (JSON Web Token)**: Estándar para la transmisión segura de información entre partes como un token firmado digitalmente.
- **Usuario Autenticado**: Persona o sistema que ha presentado credenciales válidas y posee un JWT vigente.
- **Permiso de Descarga**: Atributo del Usuario Autenticado que indica si tiene autorización para descargar guías de un transportista específico.
- **GuiaId**: Identificador único (UUID v4) asignado a cada guía en el momento de su creación.
- **Estado de Guía**: Valor que representa la etapa del ciclo de vida de una guía: `BORRADOR`, `GENERADA`, `SUBIDA`, `ERROR_SUBIDA`, `ELIMINADA`.
- **Creador de Guía**: Usuario Autenticado que invocó el endpoint de creación de una guía.
- **Pipeline**: Flujo de trabajo automatizado de GitHub Actions que construye, publica y despliega el microservicio.

---

## Requirements

---

### Requirement 1: Almacenamiento Temporal en EFS

**User Story:** Como desarrollador del sistema, quiero que las guías generadas se guarden temporalmente en el EFS montado en el microservicio, para tener un punto de almacenamiento intermedio antes de subirlas a S3.

#### Acceptance Criteria

1. WHEN el Sistema genera una guía en formato PDF, THE Sistema SHALL escribir el archivo PDF en el directorio EFS configurado mediante la variable de entorno `EFS_MOUNT_PATH`.
2. WHEN la escritura en EFS falla, THE Sistema SHALL registrar el error con nivel `ERROR` en el log incluyendo el `guiaId` y el motivo, y devolver una respuesta HTTP 500 con el `guiaId` y una descripción del fallo.
3. THE Sistema SHALL asignar un nombre de archivo único al PDF generado con el formato `{guiaId}.pdf`, para evitar colisiones entre guías distintas.
4. WHILE la guía se encuentra en estado `BORRADOR`, THE Sistema SHALL mantener el archivo PDF en el directorio EFS hasta que la subida a S3 se complete exitosamente.
5. WHEN la subida a S3 se completa exitosamente, THE Sistema SHALL eliminar el archivo PDF del directorio EFS para liberar espacio.
6. IF la eliminación del archivo en EFS falla tras una subida exitosa a S3, THE Sistema SHALL registrar el error en el log con nivel `WARN` incluyendo el `guiaId`, pero no revertir el estado `SUBIDA` ya registrado.

---

### Requirement 2: Subida Automática a Amazon S3

**User Story:** Como administrador del sistema, quiero que las guías se suban automáticamente a S3 con una ruta organizada por fecha y transportista, para facilitar la búsqueda y el archivado de documentos.

#### Acceptance Criteria

1. WHEN la generación del PDF de una guía se completa exitosamente, THE Sistema SHALL iniciar la subida del archivo a S3 almacenando el objeto en la ruta `/{YYYYMM}/{transportistaId}/{guiaId}.pdf` dentro del bucket configurado en `S3_BUCKET_NAME`, donde `YYYYMM` corresponde al año y mes de `fechaEnvio`.
2. IF la subida a S3 se completa sin errores, THEN THE Sistema SHALL actualizar el estado de la guía a `SUBIDA` y registrar la URL de acceso al objeto S3 en la base de datos; IF la actualización en base de datos falla, THE Sistema SHALL registrar el error con nivel `ERROR` preservando el estado anterior de la guía.
3. IF la subida a S3 falla por un error de red o credenciales, THEN THE Sistema SHALL reintentar la operación hasta 3 veces con un intervalo de 2 segundos entre intentos antes de considerar el intento fallido.
4. IF la subida a S3 falla con un error de red o credenciales y se han agotado los reintentos, THEN THE Sistema SHALL actualizar el estado de la guía a `ERROR_SUBIDA`; IF la subida falla con un error no reintentable (ej. bucket inexistente), THEN THE Sistema SHALL actualizar el estado a `ERROR_SUBIDA` inmediatamente sin reintentos; en ambos casos THE Sistema SHALL devolver HTTP 502 con el detalle del error.
5. THE Sistema SHALL utilizar el SDK oficial de AWS para Java (aws-sdk-java-v2) para todas las operaciones con S3, leyendo las credenciales desde las variables de entorno `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY`.
6. WHERE la región AWS esté configurada mediante la variable de entorno `AWS_REGION`, THE Sistema SHALL utilizar esa región para todas las operaciones con S3.

---

### Requirement 3: Creación de Guías de Despacho

**User Story:** Como operador de la empresa transportista, quiero crear guías de despacho mediante un endpoint REST, para registrar nuevos envíos en el sistema.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud POST a `/api/v1/guias` con un cuerpo JSON que contiene los campos obligatorios `transportistaId`, `fechaEnvio`, `destinatario` y `direccionDestino` con valores válidos, THE Sistema SHALL crear la guía con estado `BORRADOR` y devolver una respuesta HTTP 201 con el `guiaId` generado.
2. IF el cuerpo de la solicitud omite al menos uno de los campos obligatorios (`transportistaId`, `fechaEnvio`, `destinatario`, `direccionDestino`), THEN THE Sistema SHALL devolver HTTP 400 con un mensaje que enumere todos los campos faltantes.
3. THE Sistema SHALL generar un `guiaId` de tipo UUID v4 para cada guía creada, garantizando unicidad global.
4. IF el campo `fechaEnvio` no cumple con el formato ISO 8601 `YYYY-MM-DD`, THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "El campo fechaEnvio debe tener el formato YYYY-MM-DD".
5. IF el campo `transportistaId` contiene caracteres no alfanuméricos o supera los 50 caracteres, THEN THE Sistema SHALL devolver HTTP 400 con un mensaje de validación que indique la restricción incumplida.
6. THE Sistema SHALL registrar la fecha y hora de creación de la guía (`fechaCreacion`) en formato ISO 8601 UTC en el momento de la persistencia.
7. WHEN se crea una guía exitosamente, THE Sistema SHALL iniciar de forma asíncrona la generación del PDF y su escritura en EFS, sin bloquear la respuesta HTTP 201.
8. IF la persistencia de la guía en la base de datos falla, THEN THE Sistema SHALL devolver HTTP 500 con el mensaje "Error al persistir la guía" sin iniciar la generación del PDF.

---

### Requirement 4: Subida Manual de Guías a S3 mediante Endpoint

**User Story:** Como operador del sistema, quiero poder desencadenar manualmente la subida de una guía a S3 mediante un endpoint REST, para controlar cuándo se persiste el documento definitivamente.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud POST a `/api/v1/guias/{guiaId}/upload` y la guía existe en estado `BORRADOR` o `ERROR_SUBIDA` con el archivo PDF presente en EFS, THE Sistema SHALL subir el archivo desde EFS a S3 y devolver HTTP 200 con la URL del objeto subido.
2. IF la guía existe en la base de datos pero el archivo PDF no está en EFS, THEN THE Sistema SHALL devolver HTTP 404 con el mensaje "Archivo PDF no encontrado en almacenamiento temporal".
3. IF la guía identificada por `guiaId` no existe en la base de datos, THEN THE Sistema SHALL devolver HTTP 404 con el mensaje "Guía no encontrada".
4. IF la guía ya se encuentra en estado `SUBIDA`, THEN THE Sistema SHALL devolver HTTP 409 con el mensaje "La guía ya ha sido subida a S3".
5. IF la guía se encuentra en estado `ELIMINADA`, THEN THE Sistema SHALL devolver HTTP 409 con el mensaje "No es posible subir una guía eliminada".
6. IF la subida a S3 falla durante la invocación manual, THEN THE Sistema SHALL actualizar el estado de la guía a `ERROR_SUBIDA` y devolver HTTP 502 con el detalle del error.

---

### Requirement 5: Descarga de Guías con Validación de Permisos

**User Story:** Como usuario autenticado con permisos, quiero descargar una guía de despacho específica, para obtener el documento PDF en mi dispositivo.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud GET a `/api/v1/guias/{guiaId}/download`, THE Sistema SHALL validar primero el JWT en el encabezado `Authorization`; IF el JWT es válido, THE Sistema SHALL verificar los permisos del Usuario Autenticado antes de continuar.
2. WHEN el Usuario Autenticado posee el Permiso de Descarga para el `transportistaId` asociado a la guía, THE Sistema SHALL devolver HTTP 302 con un encabezado `Location` que contenga una URL pre-firmada de S3 con vigencia de 15 minutos, permitiendo que el cliente descargue el archivo directamente desde S3.
3. IF el JWT presentado en el encabezado `Authorization` está ausente, ha expirado o tiene una firma inválida, THEN THE Sistema SHALL devolver HTTP 401 con el mensaje "Token de autenticación inválido o expirado".
4. IF el Usuario Autenticado no posee el Permiso de Descarga para el `transportistaId` de la guía solicitada, THEN THE Sistema SHALL devolver HTTP 403 con el mensaje "No tiene permisos para descargar esta guía".
5. IF la guía identificada por `guiaId` no existe o se encuentra en estado `ELIMINADA`, THEN THE Sistema SHALL devolver HTTP 404 con el mensaje "Guía no encontrada".
6. IF la guía existe y el Usuario Autenticado tiene permisos, pero la guía no ha sido subida a S3 (estado distinto a `SUBIDA`), THEN THE Sistema SHALL devolver HTTP 409 con el mensaje "La guía no está disponible para descarga".
7. IF la generación de la URL pre-firmada de S3 falla, THEN THE Sistema SHALL devolver HTTP 502 con el mensaje "Error al generar enlace de descarga".

---

### Requirement 6: Modificación y Actualización de Guías

**User Story:** Como operador del sistema, quiero actualizar los datos de una guía de despacho existente, para corregir errores o actualizar información del envío.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud PUT a `/api/v1/guias/{guiaId}` con un cuerpo JSON que contiene al menos uno de los campos modificables con un valor válido, THE Sistema SHALL actualizar los campos presentes en la solicitud y devolver HTTP 200 con la guía completa actualizada.
2. THE Sistema SHALL permitir la modificación únicamente de los campos: `destinatario`, `direccionDestino`, `pesoKg`, `descripcionCarga` y `observaciones`; IF el cuerpo incluye campos no modificables (distintos a los cinco anteriores), THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "El cuerpo contiene campos no permitidos para modificación".
3. IF la guía identificada por `guiaId` no existe, THEN THE Sistema SHALL devolver HTTP 404 con el mensaje "Guía no encontrada".
4. IF la guía se encuentra en estado `ELIMINADA`, THEN THE Sistema SHALL devolver HTTP 409 con el mensaje "No es posible modificar una guía eliminada".
5. WHEN la guía se encuentra en estado `SUBIDA` y la actualización de datos es exitosa en la base de datos, THE Sistema SHALL cambiar el estado a `BORRADOR` y devolver HTTP 200 con la guía actualizada; la regeneración del PDF y re-subida a S3 ocurrirán de forma asíncrona posterior a la respuesta.
6. THE Sistema SHALL registrar la fecha y hora de la última modificación (`fechaActualizacion`) en formato ISO 8601 UTC en cada operación de actualización exitosa.
7. IF la persistencia de la actualización en base de datos falla, THEN THE Sistema SHALL devolver HTTP 500 y no modificar el estado ni los datos de la guía.

---

### Requirement 7: Eliminación de Guías

**User Story:** Como administrador del sistema, quiero eliminar guías de despacho específicas, para mantener el repositorio limpio y cumplir con políticas de retención de datos.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud DELETE a `/api/v1/guias/{guiaId}` de un Usuario Autenticado con rol `ADMIN`, THE Sistema SHALL marcar la guía como `ELIMINADA` en la base de datos y devolver HTTP 200 con el mensaje "Guía eliminada correctamente".
2. THE Sistema SHALL implementar eliminación lógica (soft delete), conservando el registro en la base de datos con estado `ELIMINADA` en lugar de borrar físicamente el registro.
3. WHEN la guía se marca como `ELIMINADA` exitosamente en la base de datos, THE Sistema SHALL intentar eliminar el objeto correspondiente en S3; la respuesta HTTP 200 se devuelve independientemente del resultado de la operación S3.
4. IF el objeto en S3 no existe al momento de la eliminación, THE Sistema SHALL continuar y completar la eliminación lógica sin devolver error, registrando en el log con nivel `INFO` que el objeto S3 no fue encontrado para el `guiaId`.
5. IF la guía identificada por `guiaId` no existe en la base de datos, THEN THE Sistema SHALL devolver HTTP 404 con el mensaje "Guía no encontrada".
6. IF la guía ya se encuentra en estado `ELIMINADA`, THEN THE Sistema SHALL devolver HTTP 409 con el mensaje "La guía ya ha sido eliminada".
7. IF el Usuario Autenticado no posee el rol `ADMIN`, THEN THE Sistema SHALL devolver HTTP 403 con el mensaje "No tiene permisos para eliminar guías".

---

### Requirement 8: Consulta de Guías por Transportista y Fecha

**User Story:** Como operador del sistema, quiero consultar las guías filtradas por transportista y fecha, para revisar el historial de envíos de forma eficiente.

#### Acceptance Criteria

1. WHEN el Sistema recibe una solicitud GET a `/api/v1/guias` con los parámetros `transportistaId` y `fecha` (formato `YYYYMM`) con valores válidos, THE Sistema SHALL devolver HTTP 200 con una lista paginada en formato JSON de guías que coincidan con ambos filtros, excluyendo las que tengan estado `ELIMINADA`.
2. WHEN la lista de resultados contiene guías, THE Sistema SHALL incluir en cada elemento los campos: `guiaId`, `transportistaId`, `fechaEnvio`, `estado`, `urlS3` y `fechaCreacion`.
3. WHEN la consulta no devuelve resultados para los filtros proporcionados, THE Sistema SHALL devolver HTTP 200 con un objeto JSON que contenga un arreglo vacío `[]` en el campo `content` y metadatos de paginación con `totalElements: 0`.
4. IF el parámetro `transportistaId` está ausente en la solicitud, THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "El parámetro transportistaId es obligatorio".
5. IF el parámetro `fecha` está ausente, no cumple el formato `YYYYMM`, o el valor de MM no está en el rango 01–12, THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "El formato del parámetro fecha debe ser YYYYMM con mes entre 01 y 12".
6. THE Sistema SHALL soportar paginación mediante los parámetros opcionales `page` (entero ≥ 0, defecto: 0) y `size` (entero entre 1 y 100, defecto: 20) en todas las consultas.
7. THE Sistema SHALL devolver en la respuesta los metadatos de paginación: `totalElements`, `totalPages`, `currentPage` y `pageSize`.
8. IF el parámetro `size` es mayor a 100, THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "El parámetro size no puede superar el valor de 100".
9. IF el parámetro `page` es menor a 0 o el parámetro `size` es menor a 1, THEN THE Sistema SHALL devolver HTTP 400 con el mensaje "Los parámetros de paginación deben ser valores positivos".

---

### Requirement 9: Pipeline CI/CD con GitHub Actions y Docker Hub

**User Story:** Como DevOps del equipo, quiero que cada push a la rama principal genere y publique automáticamente una imagen Docker en Docker Hub, para tener siempre disponible la última versión del microservicio.

#### Acceptance Criteria

1. WHEN se realiza un push a la rama `main` del repositorio, THE Pipeline SHALL ejecutar primero las pruebas unitarias del proyecto; IF todas las pruebas pasan, THE Pipeline SHALL construir la imagen Docker usando el `Dockerfile` en la raíz del proyecto.
2. IF alguna prueba unitaria falla, THEN THE Pipeline SHALL marcar el job como fallido y no proceder con la construcción ni publicación de la imagen.
3. THE Pipeline SHALL autenticarse en Docker Hub usando los secretos `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN` almacenados en GitHub Secrets, sin exponer credenciales en el código fuente.
4. WHEN la construcción de la imagen Docker es exitosa, THE Pipeline SHALL publicar la imagen en Docker Hub con las etiquetas `latest` y los primeros 7 caracteres del SHA del commit actual.
5. IF la construcción de la imagen Docker falla, THEN THE Pipeline SHALL marcar el job como fallido y no publicar ninguna imagen en Docker Hub.
6. IF la publicación de la imagen en Docker Hub falla (error de red, autenticación u otro), THEN THE Pipeline SHALL marcar el job como fallido y registrar el error en el log del workflow.

---

### Requirement 10: Despliegue Automático en EC2 con GitHub Actions

**User Story:** Como DevOps del equipo, quiero que el microservicio se despliegue automáticamente en una instancia EC2 al publicar una nueva imagen en Docker Hub, para reducir el tiempo de entrega de nuevas versiones a producción.

#### Acceptance Criteria

1. WHEN la imagen Docker es publicada exitosamente en Docker Hub, THE Pipeline SHALL conectarse a la instancia EC2 mediante SSH usando la clave privada almacenada en el secreto `EC2_SSH_KEY`, el host en `EC2_HOST` y el usuario en `EC2_USER`, con un timeout de conexión de 10 segundos por intento.
2. WHEN el Pipeline se conecta a EC2, THE Pipeline SHALL ejecutar el script de despliegue que realice en orden: `docker pull` de la nueva imagen, `docker stop` del contenedor anterior (si existe), y `docker run` del nuevo contenedor.
3. THE Pipeline SHALL pasar al contenedor en EC2 las variables de entorno necesarias (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `S3_BUCKET_NAME`, `EFS_MOUNT_PATH`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) leídas desde GitHub Secrets.
4. IF la conexión SSH a EC2 falla, THEN THE Pipeline SHALL reintentar hasta 2 veces con un intervalo de 30 segundos entre intentos; IF los 3 intentos totales fallan, THE Pipeline SHALL marcar el job como fallido.
5. IF el comando `docker pull` falla en EC2 (imagen no encontrada, error de red), THEN THE Pipeline SHALL abortar el despliegue sin detener el contenedor activo y marcar el job como fallido.
6. WHEN el nuevo contenedor arranca en EC2, THE Pipeline SHALL verificar que el endpoint `/actuator/health` devuelve HTTP 200 dentro de los 60 segundos posteriores al arranque; IF la verificación es exitosa, THE Pipeline SHALL registrar en el log el ID del contenedor activo y la etiqueta de imagen desplegada.
7. IF el endpoint `/actuator/health` no devuelve HTTP 200 dentro de los 60 segundos, THEN THE Pipeline SHALL marcar el job como fallido y registrar el ID del contenedor con estado de salud no confirmado.

---

### Requirement 11: Generación de PDF de Guía de Despacho

**User Story:** Como operador del sistema, quiero que el sistema genere automáticamente el PDF de la guía con toda la información del envío, para tener un documento imprimible y compartible.

#### Acceptance Criteria

1. WHEN el Sistema crea una guía exitosamente, THE Generador_PDF SHALL generar de forma asíncrona un documento PDF que incluya: `guiaId`, `transportistaId`, `fechaEnvio`, `destinatario`, `direccionDestino`, `pesoKg`, `descripcionCarga` y, si está presente, `observaciones`.
2. WHEN el Generador_PDF produce el documento, THE Generador_PDF SHALL incluir un código QR que codifique la URL de descarga de la guía (`/api/v1/guias/{guiaId}/download`).
3. WHEN la generación del PDF inicia de forma asíncrona, THE Sistema SHALL devolver la respuesta HTTP 201 al cliente antes de que el PDF se complete; el estado de la guía permanecerá en `BORRADOR` hasta que el PDF sea generado y escrito en EFS exitosamente.
4. IF la generación del PDF falla por cualquier motivo, THEN THE Generador_PDF SHALL registrar el error con nivel `ERROR` en el log incluyendo el `guiaId` y la causa del fallo; la guía permanecerá en estado `BORRADOR` y la creación de la guía no será revertida.
5. IF el campo `observaciones` no está presente en la solicitud de creación, THE Generador_PDF SHALL generar el PDF omitiendo esa sección sin devolver error.
6. THE Generador_PDF SHALL utilizar la librería Apache PDFBox (open-source, sin costo de licencia) para la generación del PDF.

---

### Requirement 12: Documentación del Proyecto y Guía de Despliegue

**User Story:** Como desarrollador o estudiante que trabaja con el proyecto, quiero contar con documentación clara y pedagógica sobre la estructura del código, la configuración local y los pasos en AWS, para poder entender, ejecutar y desplegar el sistema con el menor número posible de créditos AWS.

#### Acceptance Criteria

1. THE Sistema SHALL incluir un archivo `README.md` en la raíz del repositorio que explique la arquitectura general del sistema, la estructura de directorios del proyecto y el propósito de cada módulo principal.
2. THE README.md SHALL incluir una sección "Requisitos Previos" que liste las herramientas necesarias con sus versiones mínimas: Java 17, Maven 3.8+, Docker 24+, AWS CLI v2 y Git 2.x.
3. THE README.md SHALL incluir una sección "Ejecución en Local" con los comandos exactos para levantar el proyecto usando Docker Compose, incluyendo una configuración de LocalStack que simule S3 localmente sin consumir créditos AWS.
4. THE README.md SHALL incluir una sección "Configuración de AWS" con los pasos numerados para: (a) crear el bucket S3, (b) configurar un punto de montaje EFS y montarlo en EC2, (c) crear un usuario IAM con política de permisos mínimos para S3 y EFS, y (d) lanzar una instancia EC2 de tipo `t2.micro` elegible para la capa gratuita.
5. THE README.md SHALL incluir una sección "Variables de Entorno" con una tabla que describa cada variable, su valor de ejemplo y si es obligatoria u opcional.
6. THE README.md SHALL incluir una sección "Pipeline CI/CD" que explique paso a paso cómo configurar los secretos de GitHub y cómo funciona el flujo automático desde el push hasta el despliegue en EC2.
7. WHERE el proyecto incluya clases Java con lógica no trivial, THE Sistema SHALL incluir comentarios Javadoc en todas las clases de servicio, repositorio y controlador explicando la responsabilidad de la clase y el contrato de cada método público.
8. THE README.md SHALL incluir una sección "Estimación de Costos AWS" que detalle cómo el uso de `t2.micro` (capa gratuita 750 h/mes), S3 estándar (5 GB gratuitos), EFS modo Infrequent Access y LocalStack para desarrollo local permite operar el sistema con menos de 30 créditos AWS mensuales en un entorno de pruebas.

---

## Notas de Implementación y Eficiencia de Costos AWS

Las siguientes indicaciones no son requisitos estrictos del sistema pero guían las decisiones de diseño para cumplir con el objetivo de menos de 30 créditos AWS:

- **EC2 t2.micro**: Usar siempre instancias `t2.micro` (1 vCPU, 1 GB RAM) elegibles para la capa gratuita de AWS (750 horas/mes el primer año).
- **S3 Capa Gratuita**: Los primeros 5 GB de almacenamiento S3 y 20,000 solicitudes GET / 2,000 PUT son gratuitos mensualmente.
- **EFS Infrequent Access**: El EFS tiene un costo por GB almacenado (~0.30 USD/GB/mes en modo estándar). Usar el modo Infrequent Access (~0.025 USD/GB/mes) y limpiar archivos tras la subida a S3 reduce significativamente el costo.
- **LocalStack para desarrollo local**: Simular S3 localmente con LocalStack evita cargos durante el desarrollo y las pruebas.
- **URLs pre-firmadas**: Usar URLs pre-firmadas de S3 para descargas evita que el tráfico de descarga pase por EC2, eliminando costos de transferencia de datos de salida del servidor.
- **Apagar EC2 fuera del horario de uso**: En ambientes de prueba, detener la instancia EC2 cuando no esté en uso reduce el consumo de créditos significativamente.
- **Base de datos H2 en local / RDS Free Tier en producción**: Usar H2 en memoria para pruebas locales y la capa gratuita de RDS (MySQL/PostgreSQL `db.t3.micro`, 750 h/mes el primer año) para producción.
