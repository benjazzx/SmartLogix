# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

COPY src src
RUN mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="SmartLogix Users Service"
LABEL org.opencontainers.image.description="Microservicio de gestión de usuarios y autenticación JWT — SmartLogix DSY1106"
LABEL org.opencontainers.image.vendor="SmartLogix"
LABEL org.opencontainers.image.version="1.0.0"

WORKDIR /app

RUN addgroup -S smartlogix && adduser -S smartlogix -G smartlogix

COPY --from=build /app/target/Users-0.0.1-SNAPSHOT.jar app.jar
RUN chown smartlogix:smartlogix app.jar

USER smartlogix

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
