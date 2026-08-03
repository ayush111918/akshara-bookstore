FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S akshara && adduser -S akshara -G akshara && mkdir -p /app/data/personal-library /app/logs && chown -R akshara:akshara /app
COPY --from=build --chown=akshara:akshara /workspace/target/akshara-api-0.0.1-SNAPSHOT.jar app.jar
USER akshara
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
