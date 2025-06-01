# Build stage - Usa JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

# Compila com Java 21
RUN mvn clean install -DskipTests

# Runtime stage - Usa JRE 21
FROM eclipse-temurin:21-jre-alpine

COPY --from=build /app/target/whatsapp-*.jar /app/app.jar

WORKDIR /app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]