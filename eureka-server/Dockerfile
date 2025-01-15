FROM gradle:8.11.1-jdk21 AS builder

WORKDIR /app

# Copy application files
COPY . .

# Run Gradle build with Java 21 compatibility
RUN gradle :eureka-server:build -x test --no-daemon --info

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar and application.yml
COPY --from=builder /app/eureka-server/build/libs/*.jar app.jar
COPY --from=builder /app/eureka-server/src/main/resources/application.yml /app/application.yml

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/application.yml"]
