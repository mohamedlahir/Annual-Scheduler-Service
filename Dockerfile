## Multi-stage Dockerfile for annual.scheduler
# Stage 1: build the application
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy maven wrapper and pom first to leverage Docker layer cache for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn

# copy source and build
COPY src ./src

RUN mvn -B -ntp -DskipTests package

# Stage 2: runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","/app/app.jar"]

