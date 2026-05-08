# ETAPA 1: Construcción (Usamos una imagen de Maven que soporta ARM64 nativo)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos solo el pom.xml para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ETAPA 2: Imagen Final (Ligera y optimizada)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el JAR generado
COPY --from=build /app/target/*.jar app.jar

# Configuración de usuario no-root por seguridad
RUN addgroup -S talentcircle && adduser -S talentcircle -G talentcircle
USER talentcircle

EXPOSE 8080

# Forzamos parámetros de JVM optimizados para contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]