# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk

# Set the working directory
WORKDIR /app

# Copy the Maven project files
COPY . .

# Build the application (try Maven wrapper, fallback to system Maven)
RUN if [ -f ./mvnw ]; then ./mvnw clean package -DskipTests; else mvn clean package -DskipTests; fi

# Expose the port (Render will map this to $PORT)
EXPOSE 8080

# Start the server, using the PORT env variable if set
CMD ["sh", "-c", "java -jar jtt808-server/target/jtt808-server-1.0.0-SNAPSHOT.jar"] 