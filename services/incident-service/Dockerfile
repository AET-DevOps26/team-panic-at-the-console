FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Build context is services/ — generated stubs must be available before the service compiles
COPY generated/java/pom.xml generated/java/pom.xml
COPY generated/java/src generated/java/src
RUN mvn install -f generated/java/pom.xml -DskipTests -q

COPY incident-service/pom.xml incident-service/pom.xml
RUN mvn dependency:go-offline -f incident-service/pom.xml -q
COPY incident-service/src incident-service/src
RUN mvn package -f incident-service/pom.xml -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/incident-service/target/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
