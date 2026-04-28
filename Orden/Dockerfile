# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Compila el JAR con Maven. Las dependencias se descargan antes de copiar el
# código fuente para que Docker reutilice esa capa en rebuilds.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

COPY src src
RUN mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="SmartLogix Orden Service"
LABEL org.opencontainers.image.description="Microservicio de gestión de órdenes — SmartLogix DSY1106"
LABEL org.opencontainers.image.vendor="SmartLogix"
LABEL org.opencontainers.image.version="1.0.0"

WORKDIR /app

# Usuario sin privilegios para mejorar la seguridad del contenedor
RUN addgroup -S smartlogix && adduser -S smartlogix -G smartlogix

COPY --from=build /app/target/Orden-0.0.1-SNAPSHOT.jar app.jar
RUN chown smartlogix:smartlogix app.jar

USER smartlogix

EXPOSE 8084

# -XX:+UseContainerSupport  → JVM respeta los límites de CPU/RAM del contenedor
# -XX:MaxRAMPercentage=75.0 → usa máximo el 75 % de la RAM asignada al contenedor
# -Djava.security.egd      → acelera el arranque en entornos sin entropía
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
