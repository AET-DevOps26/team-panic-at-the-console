FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Build context is services/ — generated sources consumed via build-helper-maven-plugin
COPY generated/java/src generated/java/src

COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn dependency:go-offline -f notification-service/pom.xml -q
COPY notification-service/src notification-service/src
RUN mvn package -f notification-service/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/target/app.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
