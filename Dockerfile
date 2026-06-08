# ─────────────────────────────────────────────
# Imagen de producción: Eclipse Temurin 17 JRE
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR generado por Maven
COPY target/guias-despacho-*.jar app.jar

# Puerto de la aplicación
EXPOSE 8080

# Punto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]
