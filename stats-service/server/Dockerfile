FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY stats-service/pom.xml stats-service/pom.xml
COPY stats-service/dto/pom.xml stats-service/dto/pom.xml
COPY stats-service/server/pom.xml stats-service/server/pom.xml
COPY stats-service/client/pom.xml stats-service/client/pom.xml
COPY ewm-main-service/pom.xml ewm-main-service/pom.xml

RUN mvn -q -pl stats-service/server -am -DskipTests dependency:go-offline

COPY stats-service/dto/src stats-service/dto/src
COPY stats-service/server/src stats-service/server/src

RUN mvn -q -pl stats-service/server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/stats-service/server/target/stats-service-server.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
