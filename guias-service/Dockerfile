# ─────────────────────────────────────────────
# Imagen de producción: Eclipse Temurin 17 JRE
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR generado por Maven
COPY target/guias-despacho-*.jar app.jar

# Puerto de la aplicación
EXPOSE 8090

# Punto de entrada — JAVA_OPTS permite inyectar flags JVM desde docker-compose
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
