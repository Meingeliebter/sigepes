# --- Etapa de compilación -------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias se resuelven antes de copiar el código para aprovechar la caché de capas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Etapa de ejecución ----------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# La JVM no se ejecuta como root: docker-entrypoint.sh prepara el volumen y baja al usuario sigepes.
RUN addgroup -S sigepes && adduser -S sigepes -G sigepes && apk add --no-cache su-exec
COPY --from=build /build/target/backend-*.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
# AC-15.6: punto de montaje del disco persistente para los archivos subidos.
RUN chmod 755 /usr/local/bin/docker-entrypoint.sh && mkdir -p /var/data/uploads && chown -R sigepes:sigepes /var/data

EXPOSE 8080
# AC-21.3: el orquestador reinicia el contenedor cuando el health check falla. PORT lo asigna la
# plataforma (Railway); en local y en docker compose es 8080.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3     CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/actuator/health" | grep -q '"UP"' || exit 1
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
