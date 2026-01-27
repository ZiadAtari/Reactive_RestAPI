# Stage 1: Build
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
# Go offline to cache dependencies
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy the Fat JAR from the build stage
COPY --from=build /app/target/ReactiveAPI-1.0.0-SNAPSHOT-fat.jar ./app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8888

# Run the application
CMD ["java", "-jar", "app.jar"]
