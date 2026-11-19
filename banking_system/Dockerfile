# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# Cache dependencies separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -DskipTests package -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
# Non-root user for security
RUN addgroup -S banking && adduser -S banking -G banking
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown banking:banking app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
