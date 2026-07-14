# =========================================================
# Etapa 1: Compilación (Build Stage)
# Docker descarga Maven + JDK y compila el código fuente.
# Esto garantiza que target/ siempre exista dentro del
# contexto Docker, sin depender del runner de GitHub Actions.
# =========================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copiar POM primero para aprovechar caché de capas Docker:
# si pom.xml no cambia, las dependencias no se vuelven a descargar
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y empaquetar
COPY src ./src
RUN mvn clean package -DskipTests -B

# =========================================================
# Etapa 2: Imagen de ejecución (Runtime Stage)
# Imagen mínima JRE Alpine — no incluye Maven ni JDK
# =========================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR desde la etapa de build (no desde el host)
COPY --from=builder /build/target/guias-despacho-*.jar app.jar

EXPOSE 8090

# JAVA_OPTS permite inyectar flags JVM desde docker-compose (ej: -Xmx160m)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
