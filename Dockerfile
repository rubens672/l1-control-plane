FROM maven:3.9.5-eclipse-temurin-21 AS builder
WORKDIR /app

# Copia i file necessari per la build
COPY pom.xml .
COPY shared/pom.xml shared/
COPY admin-service/pom.xml admin-service/
COPY enrollment-service/pom.xml enrollment-service/
COPY ingest-service/pom.xml ingest-service/
COPY shared/src shared/src
COPY admin-service/src admin-service/src
COPY enrollment-service/src enrollment-service/src
COPY ingest-service/src ingest-service/src

# Esegui la build di tutti i moduli in parallelo (skipping test per velocità in fase di image build - i test dovrebbero essere gestiti in un job separato della CI)
RUN mvn clean package -DskipTests -T 1C

# Runner Stage
FROM eclipse-temurin:21-jre-alpine
# L'argomento viene passato dal docker build o cloudbuild
ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

WORKDIR /app

# Copia solo il JAR compilato del servizio specifico
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080
# Avvia l'applicazione Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
