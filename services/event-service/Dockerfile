FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Build context is services/ — generated sources consumed via build-helper-maven-plugin
COPY generated/java/src generated/java/src

COPY event-service/pom.xml event-service/pom.xml
RUN mvn dependency:go-offline -f event-service/pom.xml -q
COPY event-service/src event-service/src
RUN mvn package -f event-service/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/event-service/target/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
