FROM gradle:8.11.1-jdk21 AS builder

WORKDIR /app

COPY . .

RUN gradle :cart-service:build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/user-service/build/libs/*.jar app.jar

COPY --from=builder /app/user-service/src/main/resources/application.yml /app/application.yml

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/application.yml"]