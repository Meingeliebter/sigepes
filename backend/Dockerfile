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
USER sigepes

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
