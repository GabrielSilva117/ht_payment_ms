FROM maven:3.9.11-eclipse-temurin-21 as build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build ./app/target/*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]