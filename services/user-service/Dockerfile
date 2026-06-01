FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Build context is services/ — generated sources consumed via build-helper-maven-plugin
COPY generated/java/src generated/java/src

COPY user-service/pom.xml user-service/pom.xml
RUN mvn dependency:go-offline -f user-service/pom.xml -q
COPY user-service/src user-service/src
RUN mvn package -f user-service/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/target/app.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
