FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

COPY generated/java/src generated/java/src

COPY gateway/pom.xml gateway/pom.xml
RUN mvn dependency:go-offline -f gateway/pom.xml -q
COPY gateway/src gateway/src
RUN mvn package -f gateway/pom.xml -DskipTests -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
