FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (leverage Docker cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Final image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built artifact
COPY --from=build /app/target/*.jar app.jar

# Create a non-root user
RUN addgroup -S talentcircle && adduser -S talentcircle -G talentcircle
USER talentcircle

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
# sistema simpe para arquitecturas amd64 o x86 