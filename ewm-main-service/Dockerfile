FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY stats-service/pom.xml stats-service/pom.xml
COPY stats-service/dto/pom.xml stats-service/dto/pom.xml
COPY stats-service/server/pom.xml stats-service/server/pom.xml
COPY stats-service/client/pom.xml stats-service/client/pom.xml
COPY ewm-main-service/pom.xml ewm-main-service/pom.xml

RUN mvn -q -pl ewm-main-service -am -DskipTests dependency:go-offline

COPY stats-service/dto/src stats-service/dto/src
COPY stats-service/client/src stats-service/client/src
COPY ewm-main-service/src ewm-main-service/src

RUN mvn -q -pl ewm-main-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/ewm-main-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
