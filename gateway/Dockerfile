# ─────────────────────────────────────────────
# Stage 1: Build con Maven
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Runtime mínimo
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# JAVA_OPTS permite inyectar flags JVM desde docker-compose (ej: -Xmx120m)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
