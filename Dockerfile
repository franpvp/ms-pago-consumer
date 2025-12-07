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

COPY --from=build /app/target/ms-pago-consumer-0.0.1-SNAPSHOT.jar app.jar

ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    KAFKA_GROUP_ID=ms-pago-consumer \
    KAFKA_TOPIC=pagos

ENTRYPOINT ["java","-jar","/app/app.jar"]