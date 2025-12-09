# ===== Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Run =====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=America/Santiago

# Copiar el wallet a la imagen
COPY Wallet_DQXABCOJF1X64NFC /app/wallet

COPY --from=build /app/target/mspagoconsumer-0.0.1-SNAPSHOT.jar app.jar

# Variables alineadas con application.properties
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    KAFKA_GROUP_ID=ms-pago-consumer-grp

ENTRYPOINT ["java","-jar","/app/app.jar"]