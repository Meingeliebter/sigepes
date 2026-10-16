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

# No se ejecuta como root.
RUN addgroup -S sigepes && adduser -S sigepes -G sigepes
COPY --from=build /build/target/backend-*.jar app.jar
# AC-15.6: punto de montaje del disco persistente para los archivos subidos.
RUN mkdir -p /var/data/uploads && chown -R sigepes:sigepes /var/data
USER sigepes

EXPOSE 8080
# AC-21.3: el orquestador reinicia el contenedor cuando el health check falla.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3     CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
