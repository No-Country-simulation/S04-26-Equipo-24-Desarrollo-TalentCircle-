# Etapa 1: Build con Maven y Java 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar el pom y descargar dependencias (para aprovechar el cache de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el jar desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

# Comando de arranque con perfil docker
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]