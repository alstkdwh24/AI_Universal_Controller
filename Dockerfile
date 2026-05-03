# =====================================================
# MembersSecurity (인증 서버 - 8086 포트)
# 빌드 컨텍스트: 프로젝트 루트
# docker build -f Dockerfile -t members-security .
# =====================================================

# 1단계: 빌드 (루트에서 모든 모듈 접근 가능)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Gradle wrapper 먼저 복사 (의존성 캐시 레이어 분리)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY settings.gradle settings.gradle

# 소스 복사 (EntityCom이 MembersSecurity 의존성이므로 함께 복사)
COPY backend/EntityCom/ backend/EntityCom/
COPY backend/MembersSecurity/ backend/MembersSecurity/

RUN chmod +x gradlew && ./gradlew :MembersSecurity:bootJar --no-daemon

# 2단계: 실행 이미지 (빌드 도구 제외해서 이미지 경량화)
FROM amazoncorretto:17-alpine-jdk
COPY --from=builder /workspace/backend/MembersSecurity/build/libs/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app.jar"]