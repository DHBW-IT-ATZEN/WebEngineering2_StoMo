# ---- Stage 1: build (dependencies cached separately from source) ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Resolve dependencies first; this layer is reused on rebuilds unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
# Then the sources. Tests run in CI, not in the image build.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime (slim JRE, non-root, health-checked) ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# curl is used by the HEALTHCHECK; create an unprivileged user to run as.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --create-home --uid 1001 stomo
COPY --from=build /app/target/*.jar app.jar
USER stomo
EXPOSE 8080

# Healthy only once Spring Boot Actuator reports UP (which includes DB connectivity).
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Container-aware heap sizing. Tune at runtime via JAVA_TOOL_OPTIONS (read automatically by the JVM).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
