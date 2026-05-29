# ─── Build Stage ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
# Pre-fetch dependencies (layer caching)
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

# ─── Runtime Stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
