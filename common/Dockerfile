FROM gradle:8.11.1-jdk17 AS builder

WORKDIR /app

COPY . .

# common 모듈 빌드
RUN gradle :common:build -x test --no-daemon

# 실제 실행에 사용할 이미지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# common JAR 파일과 설정 파일 복사
COPY --from=builder /app/common/build/libs/*.jar app.jar
COPY --from=builder /app/common/src/main/resources/application.yml /app/application.yml

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/application.yml"]
