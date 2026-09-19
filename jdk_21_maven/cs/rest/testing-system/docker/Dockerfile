FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# First run "./mvnw clean package" locally
COPY target/testing-system-0.0.1-SNAPSHOT.jar /app/target/testing-system.jar

# Expose port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/target/testing-system.jar"]
