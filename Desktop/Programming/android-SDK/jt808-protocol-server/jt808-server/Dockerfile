# Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Install Maven and curl for health checks
RUN apt-get update && apt-get install -y maven curl && rm -rf /var/lib/apt/lists/*

# Copy pom.xml files first for better caching
COPY pom.xml ./
COPY commons/pom.xml ./commons/
COPY jtt808-protocol/pom.xml ./jtt808-protocol/
COPY jtt808-server/pom.xml ./jtt808-server/

# Copy source code
COPY . .

# Build the application (skip dependency resolution step that causes issues)
RUN mvn clean package -DskipTests -B

# Create data directory for H2 database
RUN mkdir -p /app/data

# Expose ports
EXPOSE 8100 7100 7200

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PORT=8100
ENV JT808_TCP_PORT=7100
ENV JT808_UDP_PORT=7100

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8100/actuator/health || exit 1

# Start the application
CMD ["java", "-jar", "jtt808-server/target/jtt808-server-1.0.0-SNAPSHOT.jar"] 