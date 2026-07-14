FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Build context is services/ — generated sources consumed via build-helper-maven-plugin
COPY generated/java/src generated/java/src

COPY webhook-service/pom.xml webhook-service/pom.xml
RUN mvn dependency:go-offline -f webhook-service/pom.xml -q
COPY webhook-service/src webhook-service/src
RUN mvn package -f webhook-service/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/webhook-service/target/app.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
