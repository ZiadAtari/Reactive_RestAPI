# Build Stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /home/app
COPY src /home/app/src
COPY pom.xml /home/app
COPY private_key.pem /home/app
COPY public_key.pem /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre
COPY --from=build /home/app/target/*.jar /usr/local/lib/demo.jar
COPY --from=build /home/app/public_key.pem /usr/local/lib/public_key.pem

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/local/lib/demo.jar"]
