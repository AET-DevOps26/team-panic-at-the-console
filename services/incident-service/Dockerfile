FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Build context is services/ — generated sources consumed via build-helper-maven-plugin
COPY generated/java/src generated/java/src

COPY incident-service/pom.xml incident-service/pom.xml
RUN mvn dependency:go-offline -f incident-service/pom.xml -q
COPY incident-service/src incident-service/src
RUN mvn package -f incident-service/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/incident-service/target/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
