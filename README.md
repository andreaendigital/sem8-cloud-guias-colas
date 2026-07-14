# sem8 — Cloud Native: Guías de Despacho con Mensajería Asíncrona

Link al video: https://youtu.be/pRO9_dyJetc
Link al informe: 
Adicionalmente se presenta documentación en este readme

## Estado actual de avance
14/07/26  2:55 am

Se completó el desarrollo e implementación de los tres microservicios y la infraestructura de mensajería. Durante las pruebas de integración en EC2 se logró acceder correctamente a la interfaz de administración de RabbitMQ (`http://IP:15672`), se verificaron las colas, el exchange y los bindings declarados automáticamente por Spring AMQP.

Sin embargo, al intentar probar los endpoints de los microservicios `guias-service` y `consumer` desde Postman, se detectó que la mitad de los contenedores no estaban corriendo. La causa fue que el driver JDBC de PostgreSQL (`org.postgresql:postgresql`) no estaba incluido en los `pom.xml` de dichos servicios — solo se había declarado el conector MySQL. Esto provocó un `ClassNotFoundException: org.postgresql.Driver` al arrancar, impidiendo que Spring Boot iniciara el contexto de base de datos.

La corrección fue agregar la dependencia en ambos `pom.xml`, lo que requirió un nuevo ciclo de build y deploy para que las imágenes Docker se actualizaran con el driver incluido.

---

## Descripción del proyecto

Sistema de gestión de **Guías de Despacho** implementado como monorepo con arquitectura de microservicios y comunicación asíncrona mediante RabbitMQ. Desplegado en AWS EC2 con CI/CD automatizado vía GitHub Actions.

---

## Microservicios

| Servicio | Puerto | Descripción |
|---|---|---|
| `guias-service` | 8080 | CRUD de guías, generación de PDF, subida a S3 |
| `producer` | 8081 | Publica eventos de guías en RabbitMQ |
| `consumer` | 8082 | Consume mensajes y persiste eventos en BD |
| `rabbitmq` | 5672 / 15672 | Message broker (AMQP + consola web) |

---

## Tecnologías

- **Java 17** + Spring Boot 3.2.5
- **Spring AMQP** (RabbitMQ) — mensajería asíncrona
- **Spring Security** + OAuth2 Resource Server — autenticación con Azure AD B2C
- **Spring Data JPA** — persistencia con PostgreSQL (RDS) / H2 (local)
- **Docker** + Docker Compose — contenedores
- **GitHub Actions** — CI/CD (build, push a Docker Hub, deploy en EC2)
- **AWS EC2** — instancia de producción
- **AWS S3** — almacenamiento de PDFs de guías

---

## Arquitectura de mensajería (RabbitMQ)

```
guias-service / producer
        │
        ▼
 guias.exchange (Direct)
        │
        ├── routing key: guias.routing.key ──────► guias.queue       → consumer
        │
        └── routing key: guias.error.routing.key ► guias.error.queue → DLQ
```

Los exchanges, colas y bindings se declaran automáticamente al arrancar los microservicios mediante los beans `@Configuration` de Spring AMQP — no requieren configuración manual en la consola de RabbitMQ.

---

## Seguridad

Autenticación con **Azure AD B2C** mediante tokens JWT. Dos roles definidos como custom attributes en el tenant `duocrosero`:

| Claim | Valor | Acceso |
|---|---|---|
| `extension_consultaRole` | `admin` | Todos los endpoints |
| `extension_consultaRole` | `transportista` | Solo consulta y descarga |

---

## Estructura del repositorio

```
sem8/
├── guias-service/       # Microservicio principal de guías
├── producer/            # Microservicio publicador de mensajes
├── consumer/            # Microservicio consumidor de mensajes
├── rabbitmq/            # Dockerfile y configuración de RabbitMQ
│   ├── Dockerfile
│   └── conf/rabbitmq.conf
├── docker-compose.yml       # Desarrollo local (build desde código)
├── docker-compose.prod.yml  # Producción (imágenes Docker Hub)
└── .github/workflows/
    └── ci-cd.yml        # Pipeline CI/CD
```

---

## Variables de entorno requeridas (GitHub Secrets)

| Secret | Descripción |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub |
| `EC2_HOST` | IP o DNS del EC2 |
| `EC2_USERNAME` | Usuario SSH del EC2 (`ec2-user`) |
| `EC2_SSH_KEY` | Clave privada SSH (.pem) |
| `DB_URL` | URL de conexión RDS PostgreSQL |
| `DB_USERNAME` | Usuario de la BD |
| `DB_PASSWORD` | Contraseña de la BD |
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS |
| `AWS_SESSION_TOKEN` | Token de sesión AWS Academy |
| `AWS_REGION` | Región AWS (ej. `us-east-1`) |
| `S3_BUCKET_NAME` | Nombre del bucket S3 |
| `RABBITMQ_USERNAME` | Usuario RabbitMQ |
| `RABBITMQ_PASSWORD` | Contraseña RabbitMQ |
| `AZURE_TENANT_ID` | Tenant ID de Azure AD B2C |
| `AZURE_CLIENT_ID` | Client ID de la app registrada |
| `AZURE_CLIENT_SECRET` | Client secret de la app |

---

## Ejecución local

```bash
# Clonar el repositorio
git clone https://github.com/andreaendigital/sem8-cloud-guias-colas.git
cd sem8

# Levantar todos los servicios (requiere Docker y Docker Compose)
docker compose up --build
```

Servicios disponibles en local:
- guias-service: `http://localhost:8080/swagger-ui.html`
- producer: `http://localhost:8081/swagger-ui.html`
- consumer: `http://localhost:8082/swagger-ui.html`
- RabbitMQ UI: `http://localhost:15672` (guest / guest)
