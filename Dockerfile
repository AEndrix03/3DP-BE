# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

# Costruisce un JAR eseguibile senza toccare il POM
RUN mvn clean package spring-boot:repackage -DskipTests

# Secondo stage: immagine leggera per esecuzione
FROM eclipse-temurin:21-jre-alpine

ENV PRAETOR_PROFILE=docker

WORKDIR /app

# Copia il JAR dallo stage precedente e lo rinomina
COPY --from=builder /app/target/*.jar printer.jar

# Permette override da esterno (es. variabili o docker-compose)
ENV JAVA_TOOL_OPTIONS="-Xmx128m -Xms64m -XX:+UseSerialGC"

ENTRYPOINT ["java", "-jar", "printer.jar"]

