# ==============================================================================
# MediCare AI - Production Multi-Stage Container Image
# Runtime: Eclipse Temurin OpenJDK 25 (LTS)
# Framework: Spring Boot 4.1.2
# ==============================================================================

# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace

# Install Maven
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy project source files
COPY src ./src

# Build production executable JAR
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal, secure runtime stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runner
WORKDIR /app

# Install curl for container healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create unprivileged service user and group
RUN groupadd -g 1001 medicare && \
    useradd -u 1001 -g medicare -m -s /bin/bash medicare

# Create runtime directories for local persistence and grant permissions
RUN mkdir -p /app/data && chown -R medicare:medicare /app

# Copy executable jar from builder stage
COPY --from=builder --chown=medicare:medicare /workspace/target/medicare-ai-0.0.1-SNAPSHOT.jar app.jar

# Drop root privileges
USER medicare

# Expose HTTP port
EXPOSE 8080

# Production JVM optimizations
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Container Healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=35s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Launch application
ENTRYPOINT ["java", "-jar", "app.jar"]
