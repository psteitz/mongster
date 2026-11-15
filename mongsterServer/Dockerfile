# ============================================================================
# Mongster Docker Image - Multi-stage Build
# ============================================================================
# Produces a small, production-ready image with quality gates built in
# ============================================================================

# Stage 1: Build stage - compile and package the application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build/mongsterServer

# Copy Maven configuration files
COPY pom.xml .

# Copy source code
COPY src/main/java/ ./src/main/java/

# Build the application with quality gates (Checkstyle, PMD)
ARG JAR_FILE=target/mongster-0.1-SNAPSHOT.jar
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn clean package -B && \
    ls -la target/*.jar

# Stage 2: Runtime stage - minimal Java runtime
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="psteitz"
LABEL description="Mongster - MongoDB-backed SMTP Mail Server"
LABEL version="0.1-SNAPSHOT"
LABEL org.opencontainers.image.source=https://github.com/psteitz/mongster

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /build/mongsterServer/target/mongster-0.1-SNAPSHOT.jar ./mongster-0.1-SNAPSHOT.jar

# Create non-root user for security (UID 1000)
RUN addgroup -S mongster && \
    adduser -S mongster -G mongster -h /home/mongster -u 1000 && \
    chown -R mongster:mongster /app

USER mongster

# Default SMTP port is 2567 (can be overridden via docker run --add-host)
EXPOSE 2567

# Start the application
ENTRYPOINT ["java", "-jar", "mongster-0.1-SNAPSHOT.jar"]
