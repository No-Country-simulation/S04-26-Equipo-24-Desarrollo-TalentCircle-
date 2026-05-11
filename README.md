<div align="center">

# ✦ TalentCircle · Pipeline Editorial Inteligente

**Sistema automatizado de generación y gestión de contenido comunitario potenciado por IA**

![Version](https://img.shields.io/badge/versión-1.0.0-f5a623?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/licencia-MIT-52d68a?style=for-the-badge)

[Descripción](#-descripción) · [Arquitectura](#-arquitectura) · [Tecnologías](#-stack-tecnológico) · [Requisitos](#-requisitos-previos) · [Instalación](#-instalación-y-configuración) · [Ejecución](#-ejecución-del-proyecto) · [API Docs](#-documentación-de-la-api) · [Estructura](#-estructura-del-proyecto)

</div>

---

## 📋 Descripción

**TalentCircle** es una plataforma SaaS que automatiza el flujo editorial de contenido técnico generado en comunidades. Cada viernes, el sistema:

1. **Recolecta** automáticamente la actividad semanal de plataformas comunitarias (Discord, Circle.so, Slack)
2. **Analiza** las contribuciones usando un LLM (Claude / GPT-4) para identificar los temas más relevantes
3. **Genera** borradores diferenciados por canal: Newsletter (800-1200 palabras), LinkedIn (150-300 palabras) y Twitter/X (≤280 caracteres)
4. **Presenta** los borradores en un panel editorial donde el editor puede revisar, editar, aprobar y publicar

### Problema que resuelve

> El proceso editorial manual consume **4-6 horas semanales** y con frecuencia se pierde contenido valioso. TalentCircle reduce ese tiempo a **menos de 30 minutos** de revisión humana.

---

## 🏗 Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TALENTCIRCLE SYSTEM                          │
│                                                                     │
│  ┌──────────┐    ┌──────────────────────────────────────────────┐  │
│  │  React   │    │              Spring Boot API                  │  │
│  │  Frontend│◄──►│                                              │  │
│  │  (Vite)  │    │  ┌─────────┐ ┌──────────┐ ┌─────────────┐  │  │
│  └──────────┘    │  │Community│ │    AI    │ │    Draft    │  │  │
│                  │  │Collector│ │ Analyzer │ │  Generator  │  │  │
│  ┌──────────┐    │  └────┬────┘ └────┬─────┘ └──────┬──────┘  │  │
│  │  Quartz  │    │       │           │               │          │  │
│  │ Scheduler│───►│  ┌────▼───────────▼───────────────▼──────┐  │  │
│  └──────────┘    │  │          Pipeline Orchestrator         │  │  │
│                  │  └───────────────────┬────────────────────┘  │  │
│                  └──────────────────────┼───────────────────────┘  │
│                                         │                          │
│  ┌──────────────┐   ┌───────────────────▼──────────────────────┐  │
│  │  LLM APIs    │   │              PostgreSQL 16                 │  │
│  │  Claude /    │   │  weekly_executions │ drafts               │  │
│  │  OpenAI      │   │  community_activities │ publications      │  │
│  └──────────────┘   └──────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐   │
│  │ Discord API  │   │ LinkedIn API │   │  Circle.so API       │   │
│  └──────────────┘   └──────────────┘   └──────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Patrón de Arquitectura

El backend sigue **Arquitectura Hexagonal (Ports & Adapters)**:

```
domain/          ← Lógica de negocio pura (sin dependencias externas)
  model/         ← Entidades y Value Objects
  port/in/       ← Interfaces de casos de uso (input ports)
  port/out/      ← Interfaces de repositorios y clientes (output ports)

application/     ← Casos de uso / Servicios de aplicación

adapter/
  in/web/        ← Controladores REST (Spring MVC)
  in/scheduler/  ← Jobs Quartz / Spring Scheduler
  out/persistence/ ← Repositorios JPA
  out/llm/       ← Cliente HTTP → Claude / OpenAI
  out/linkedin/  ← Cliente HTTP → LinkedIn API v2
  out/community/ ← Clientes Discord, Circle.so, Slack
```

---

## 🛠 Stack Tecnológico

### Backend

| Tecnología | Versión | Uso |
|---|---|---|
| **Java** | 21 (LTS) | Lenguaje principal |
| **Spring Boot** | 3.3.x | Framework web y DI |
| **Spring Data JPA** | 3.3.x | ORM / persistencia |
| **Spring Security** | 6.x | Autenticación y autorización |
| **Spring Batch** | 5.x | Jobs de recolección |
| **Quartz Scheduler** | 2.3.x | Programación de tareas |
| **Hibernate** | 6.x | Implementación JPA |
| **PostgreSQL Driver** | 42.x | Conector JDBC |
| **Springdoc OpenAPI** | 2.x | Documentación de API |
| **jjwt** | 0.12.x | Generación y validación JWT |
| **Lombok** | 1.18.x | Reducción de boilerplate |
| **MapStruct** | 1.5.x | Mapeo de DTOs |
| **Bucket4j** | 8.x | Rate limiting |

### Frontend

| Tecnología | Versión | Uso |
|---|---|---|
| **React** | 18.3.x | UI Framework |
| **Vite** | 5.x | Build tool y dev server |
| **React Router** | 6.x | Enrutamiento SPA |
| **Zustand** | 4.x | Estado global |
| **Lucide React** | 0.383.x | Iconografía |
| **CSS Modules** | — | Estilos encapsulados |

### Infraestructura

| Tecnología | Versión | Uso |
|---|---|---|
| **PostgreSQL** | 16 | Base de datos principal |
| **Docker** | 24+ | Contenerización |
| **Docker Compose** | 2.x | Orquestación local |
| **GitHub Actions** | — | CI/CD |

### APIs Externas

| Servicio | Propósito |
|---|---|
| **Anthropic Claude API** | Análisis y generación de contenido (LLM principal) |
| **OpenAI API** | LLM alternativo (GPT-4o) |
| **LinkedIn API v2** | Publicación directa de posts |
| **Discord API** | Recolección de actividad comunitaria |
| **Circle.so API** | Recolección de actividad comunitaria |

---

## ✅ Requisitos Previos

Asegúrate de tener instaladas las siguientes herramientas **antes de comenzar**:

| Herramienta | Versión Mínima | Verificar |
|---|---|---|
| **Java JDK** | 21+ | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Node.js** | 18+ | `node -v` |
| **npm** | 9+ | `npm -v` |
| **Docker Desktop** | 24+ | `docker -v` |
| **Docker Compose** | 2.x | `docker compose version` |
| **Git** | 2.x | `git --version` |

> **Windows:** Se recomienda usar **Git Bash** o **WSL2** para ejecutar los comandos de este README.

---

## 📥 Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/talentcircle/pipeline-editorial.git
cd pipeline-editorial
```

El repositorio tiene la siguiente estructura raíz:

```
pipeline-editorial/
├── backend/          ← Spring Boot
├── frontend/         ← React + Vite
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
└── README.md
```

### 2. Configurar variables de entorno

Copia el archivo de ejemplo y edita con tus credenciales reales:

```bash
cp .env.example .env
```

Contenido del archivo `.env`:

```env
# ── Base de Datos ─────────────────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=talentcircle_db
DB_USERNAME=tc_user
DB_PASSWORD=tc_secret_password

# ── JWT ───────────────────────────────────────────────────────────
JWT_SECRET=tu_clave_secreta_minimo_256_bits_aqui_cambiar_en_produccion
JWT_EXPIRATION_MS=28800000    # 8 horas

# ── LLM (elige uno o configura ambos) ────────────────────────────
ANTHROPIC_API_KEY=sk-ant-api03-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
LLM_PROVIDER=ANTHROPIC        # ANTHROPIC | OPENAI
LLM_MODEL=claude-sonnet-4-5-20251001

# ── LinkedIn API ──────────────────────────────────────────────────
LINKEDIN_CLIENT_ID=xxxxxxxxxxxxxxxx
LINKEDIN_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
LINKEDIN_ACCESS_TOKEN=AQXxxxxxxxxxxxxxxxxxxxxxxxx

# ── Plataformas de Comunidad ──────────────────────────────────────
DISCORD_BOT_TOKEN=MTxxxxxxxxxxxxxxxxxxxxxxxx.Gxxxxx.xxxxxxxxxxxxx
DISCORD_GUILD_ID=123456789012345678
CIRCLE_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
CIRCLE_COMMUNITY_ID=12345

# ── Scheduler ─────────────────────────────────────────────────────
PIPELINE_CRON=0 0 18 * * FRI    # Cada viernes a las 18:00
PIPELINE_MAX_ITEMS=20

# ── Servidor ──────────────────────────────────────────────────────
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

> ⚠️ **Nunca** subas el archivo `.env` al repositorio. Está incluido en `.gitignore`.

---

## 🚀 Ejecución del Proyecto

Tienes tres formas de ejecutar el proyecto:

---

### Opción A — Docker Compose (Recomendada ✅)

Levanta toda la infraestructura (BD + Backend + Frontend) con un solo comando:

```bash
# Construir e iniciar todos los servicios
docker compose up --build

# En segundo plano (detached)
docker compose up --build -d

# Ver logs en tiempo real
docker compose logs -f

# Detener todo
docker compose down

# Detener y eliminar volúmenes (resetea la BD)
docker compose down -v
```

Servicios disponibles tras el arranque:

| Servicio | URL |
|---|---|
| Frontend React | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

---

### Opción B — Ejecución Manual (Desarrollo)

#### 2.1 Levantar PostgreSQL con Docker

```bash
docker compose -f docker-compose.dev.yml up -d postgres
```

O instala PostgreSQL localmente y crea la base de datos:

```sql
CREATE DATABASE talentcircle_db;
CREATE USER tc_user WITH ENCRYPTED PASSWORD 'tc_secret_password';
GRANT ALL PRIVILEGES ON DATABASE talentcircle_db TO tc_user;
```

#### 2.2 Backend — Spring Boot

```bash
cd backend

# Instalar dependencias y compilar
mvn clean install -DskipTests

# Ejecutar en modo desarrollo
mvn spring-boot:run

# Con perfil específico
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Compilar JAR ejecutable
mvn clean package -DskipTests
java -jar target/talentcircle-backend-1.0.0.jar
```

El backend inicia en: `http://localhost:8080`

#### 2.3 Frontend — React + Vite

```bash
cd frontend

# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm run dev

# Compilar para producción
npm run build

# Previsualizar build de producción
npm run preview
```

El frontend inicia en: `http://localhost:5173`

---

### Opción C — Makefile (Atajos de Comandos)

Si tienes `make` instalado:

```bash
make install      # Instala todas las dependencias
make dev          # Levanta backend + frontend en modo desarrollo
make build        # Compila backend y frontend para producción
make docker-up    # Inicia con Docker Compose
make docker-down  # Detiene Docker Compose
make test         # Ejecuta todos los tests
make lint         # Linting de frontend y backend
make clean        # Limpia artefactos de compilación
```

---

## ⚙️ Configuración Detallada del Backend

### application.yml

```yaml
# backend/src/main/resources/application.yml

spring:
  application:
    name: talentcircle-pipeline

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:talentcircle_db}
    username: ${DB_USERNAME:tc_user}
    password: ${DB_PASSWORD:tc_secret_password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate           # validate | update | create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: ${SERVER_PORT:8080}

# ── JWT ──────────────────────────────────────────────────
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:28800000}

# ── LLM ──────────────────────────────────────────────────
  llm:
    provider: ${LLM_PROVIDER:ANTHROPIC}
    model: ${LLM_MODEL:claude-sonnet-4-5-20251001}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      base-url: https://api.anthropic.com/v1
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1

# ── Pipeline ─────────────────────────────────────────────
  pipeline:
    cron: ${PIPELINE_CRON:0 0 18 * * FRI}
    max-items-per-execution: ${PIPELINE_MAX_ITEMS:20}
    timezone: America/Bogota

# ── LinkedIn ─────────────────────────────────────────────
  linkedin:
    client-id: ${LINKEDIN_CLIENT_ID}
    client-secret: ${LINKEDIN_CLIENT_SECRET}
    access-token: ${LINKEDIN_ACCESS_TOKEN}

# ── CORS ─────────────────────────────────────────────────
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}

# ── Swagger ──────────────────────────────────────────────
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

### Perfiles de Spring

```bash
# Desarrollo (logs verbose, H2 en memoria opcional)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Producción (logs reducidos, SSL habilitado)
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Testing (BD en memoria)
mvn test -Dspring.profiles.active=test
```

---

## ⚙️ Configuración Detallada del Frontend

### Variables de entorno Vite

```bash
# frontend/.env.local   (desarrollo — no subir al repo)
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_NAME=TalentCircle
VITE_APP_VERSION=1.0.0

# frontend/.env.production   (producción)
VITE_API_BASE_URL=https://api.talentcircle.com/api/v1
```

### vite.config.js

```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 🗄 Base de Datos

### Migraciones con Flyway

Las migraciones se ejecutan automáticamente al iniciar el backend. Se encuentran en:

```
backend/src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_community_sources.sql
├── V3__create_weekly_executions.sql
├── V4__create_community_activities.sql
├── V5__create_ai_analyses.sql
├── V6__create_drafts.sql
├── V7__create_draft_versions.sql
├── V8__create_draft_sources.sql
├── V9__create_publications.sql
├── V10__create_pipeline_configs.sql
└── V11__insert_default_config.sql
```

### Crear admin inicial

```bash
# Con el backend corriendo, ejecuta:
curl -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@talentcircle.com",
    "password": "Admin1234!",
    "fullName": "Administrador",
    "setupKey": "tc-setup-2025"
  }'
```

> El `setupKey` se define en `application.yml` bajo `app.setup.key`. Este endpoint se desactiva tras la primera ejecución.

---

## 📡 Documentación de la API

### Acceso a Swagger UI

Con el backend corriendo, visita:

```
http://localhost:8080/swagger-ui.html
```

### Endpoints principales

#### Autenticación

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "editor@talentcircle.com",
  "password": "tu_password"
}
```

Respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 28800000,
  "user": {
    "id": "uuid",
    "email": "editor@talentcircle.com",
    "role": "EDITOR"
  }
}
```

#### Borradores

```http
# Listar borradores con filtros
GET /api/v1/drafts?week=2025-04-21&channel=LINKEDIN&status=PENDING&page=0&size=10

# Aprobar borrador
POST /api/v1/drafts/{id}/approve

# Rechazar borrador
POST /api/v1/drafts/{id}/reject
{ "reason": "Motivo del rechazo" }

# Publicar borrador aprobado
POST /api/v1/drafts/{id}/publish

# Exportar borradores (JSON)
GET /api/v1/drafts/export?week=2025-04-21&format=json
```

#### Pipeline

```http
# Ejecutar pipeline manualmente (solo ADMIN)
POST /api/v1/executions/trigger

# Historial de ejecuciones
GET /api/v1/executions?page=0&size=10
```

### Autenticación en todas las peticiones

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 🧪 Ejecución de Tests

### Backend (JUnit 5 + Mockito + Testcontainers)

```bash
cd backend

# Todos los tests
mvn test

# Solo tests unitarios
mvn test -Dtest="*UnitTest"

# Solo tests de integración
mvn test -Dtest="*IntegrationTest"

# Con reporte de cobertura (JaCoCo)
mvn verify

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

**Cobertura mínima requerida:** 80%

### Frontend

```bash
cd frontend

# Tests unitarios con Vitest
npm run test

# Tests con cobertura
npm run test:coverage

# Tests en modo watch
npm run test:watch
```

---

## 🐳 Configuración Docker

### docker-compose.yml (Producción)

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${DB_USERNAME}" ]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    env_file: .env
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_HOST: postgres

  frontend:
    build:
      context: ./frontend
      dockerfile: frontend/talentcircle-app/Dockerfile
      args:
        VITE_API_BASE_URL: http://localhost:8080/api/v1
    ports:
      - "5173:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### Dockerfiles

**Backend** (`backend/Dockerfile`):

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/talentcircle-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend** (`frontend/Dockerfile`):

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## 📁 Estructura del Proyecto

```
pipeline-editorial/
│
├── 📄 README.md
├── 📄 .env.example
├── 📄 docker-compose.yml
├── 📄 docker-compose.dev.yml
├── 📄 Makefile
│
├── 📂 backend/                          ← Spring Boot 3
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/talentcircle/
│       │   ├── 📂 domain/
│       │   │   ├── model/               ← Entidades de dominio
│       │   │   │   ├── WeeklyExecution.java
│       │   │   │   ├── CommunityActivity.java
│       │   │   │   ├── AiAnalysis.java
│       │   │   │   ├── Draft.java
│       │   │   │   ├── DraftVersion.java
│       │   │   │   ├── DraftSource.java
│       │   │   │   ├── Publication.java
│       │   │   │   ├── User.java
│       │   │   │   └── CommunitySource.java
│       │   │   ├── port/in/             ← Interfaces de casos de uso
│       │   │   └── port/out/            ← Interfaces de repositorios
│       │   │
│       │   ├── 📂 application/
│       │   │   └── service/             ← Implementación de servicios
│       │   │       ├── PipelineOrchestratorService.java
│       │   │       ├── CommunityCollectorService.java
│       │   │       ├── AiAnalyzerService.java
│       │   │       ├── DraftGeneratorService.java
│       │   │       ├── DraftReviewService.java
│       │   │       ├── PublisherService.java
│       │   │       └── AuthService.java
│       │   │
│       │   ├── 📂 adapter/
│       │   │   ├── in/web/              ← Controladores REST
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── DraftController.java
│       │   │   │   ├── ExecutionController.java
│       │   │   │   └── AdminController.java
│       │   │   ├── in/scheduler/        ← Jobs automáticos
│       │   │   │   └── PipelineScheduler.java
│       │   │   ├── out/persistence/     ← Repositorios JPA
│       │   │   ├── out/llm/             ← Cliente IA (Claude/OpenAI)
│       │   │   ├── out/linkedin/        ← Cliente LinkedIn API
│       │   │   └── out/community/       ← Clientes Discord/Circle/Slack
│       │   │
│       │   ├── 📂 config/               ← Configuración de Spring Beans
│       │   ├── 📂 shared/dto/           ← DTOs de request/response
│       │   └── 📂 shared/exception/     ← Excepciones y handlers
│       │
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/            ← Scripts Flyway
│
└── 📂 frontend/                         ← React 18 + Vite 5
    ├── package.json
    ├── vite.config.js
    ├── Dockerfile
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── index.css                    ← Variables CSS globales
        │
        ├── 📂 store/
        │   └── useAppStore.js           ← Estado global Zustand
        │
        ├── 📂 router/
        │   └── AppRouter.jsx            ← React Router v6
        │
        ├── 📂 components/               ← Componentes reutilizables
        │   ├── Layout.jsx
        │   ├── Sidebar.jsx
        │   ├── Topbar.jsx
        │   ├── Toast.jsx
        │   └── DraftModal.jsx
        │
        └── 📂 pages/
            ├── Login/                   ← Pantalla de autenticación
            ├── Dashboard/               ← Panel principal
            ├── Drafts/                  ← Gestión de borradores
            ├── Executions/              ← Historial del pipeline
            └── Admin/                   ← Administración del sistema
```

---

## 👥 Usuarios del Sistema

| Rol | Email de demo | Contraseña | Permisos |
|---|---|---|---|
| **EDITOR** | `editor@talentcircle.com` | `password123` | Ver, editar, aprobar/rechazar borradores, exportar |
| **ADMIN** | `admin@talentcircle.com` | `Admin1234!` | Todo lo anterior + gestión de usuarios, fuentes y configuración |

---

## 🔄 Flujo del Pipeline

```
┌─────────────────────────────────────────────────────────┐
│  Viernes 18:00 — Scheduler activa el pipeline           │
│                                                         │
│  1. RECOLECCIÓN (≈20 min)                               │
│     └─ Consulta Discord, Circle.so, Slack               │
│     └─ Persiste CommunityActivity en PostgreSQL         │
│                                                         │
│  2. ANÁLISIS IA (≈15 min)                               │
│     └─ Construye prompt con actividades                  │
│     └─ Llama a Claude / GPT-4                           │
│     └─ Persiste AiAnalysis con scores                   │
│                                                         │
│  3. GENERACIÓN DE BORRADORES (≈5 min)                   │
│     └─ Newsletter  (800–1200 palabras)                  │
│     └─ LinkedIn    (150–300 palabras)                   │
│     └─ Twitter/X   (≤280 caracteres)                    │
│     └─ Estado: PENDING                                  │
│                                                         │
│  4. NOTIFICACIÓN (email / Slack al editor)              │
│                                                         │
│  Lunes — Editor abre el panel                           │
│     └─ Revisa y edita borradores                        │
│     └─ Aprueba → APPROVED                               │
│     └─ Publica → PUBLISHED (LinkedIn API / exporta)     │
└─────────────────────────────────────────────────────────┘
```

---

## 🚨 Solución de Problemas Frecuentes

### `vite` no se reconoce como comando (Windows)

```bash
# Opción 1 — Reinstalar node_modules
rmdir /s /q node_modules
npm install
npm run dev

# Opción 2 — Usar npx directamente
npx vite

# Opción 3 — Política de ejecución en PowerShell (como Administrador)
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Error de conexión a la base de datos

```bash
# Verifica que PostgreSQL esté corriendo
docker compose ps

# Verifica las variables de entorno
cat .env | grep DB_

# Prueba la conexión directamente
psql -h localhost -U tc_user -d talentcircle_db
```

### El pipeline no se ejecuta automáticamente

```bash
# Verifica el cron en .env
PIPELINE_CRON=0 0 18 * * FRI    # Expresión: segundo minuto hora día mes díaSemana

# Ejecuta manualmente vía API (con token ADMIN)
curl -X POST http://localhost:8080/api/v1/executions/trigger \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### Error 401 Unauthorized en la API

```bash
# Obtén un token fresco
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"editor@talentcircle.com","password":"password123"}'
```

### Puerto 8080 o 5173 ocupado

```bash
# Linux / Mac
lsof -ti:8080 | xargs kill -9
lsof -ti:5173 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

## 📊 Variables de Entorno — Referencia Completa

| Variable | Requerida | Valor por defecto | Descripción |
|---|---|---|---|
| `DB_HOST` | ✅ | `localhost` | Host de PostgreSQL |
| `DB_PORT` | ✅ | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | ✅ | `talentcircle_db` | Nombre de la base de datos |
| `DB_USERNAME` | ✅ | `tc_user` | Usuario de PostgreSQL |
| `DB_PASSWORD` | ✅ | — | Contraseña de PostgreSQL |
| `JWT_SECRET` | ✅ | — | Clave secreta JWT (mín. 256 bits) |
| `JWT_EXPIRATION_MS` | ❌ | `28800000` | Expiración del token (ms) |
| `ANTHROPIC_API_KEY` | ⚠️ | — | API Key de Anthropic Claude |
| `OPENAI_API_KEY` | ⚠️ | — | API Key de OpenAI |
| `LLM_PROVIDER` | ✅ | `ANTHROPIC` | `ANTHROPIC` o `OPENAI` |
| `LLM_MODEL` | ❌ | `claude-sonnet-4-5-20251001` | Modelo LLM a usar |
| `LINKEDIN_ACCESS_TOKEN` | ⚠️ | — | Token de LinkedIn (para publicar) |
| `DISCORD_BOT_TOKEN` | ⚠️ | — | Token del bot de Discord |
| `CIRCLE_API_KEY` | ⚠️ | — | API Key de Circle.so |
| `PIPELINE_CRON` | ❌ | `0 0 18 * * FRI` | Expresión cron del scheduler |
| `PIPELINE_MAX_ITEMS` | ❌ | `20` | Máx. actividades por ejecución |
| `SERVER_PORT` | ❌ | `8080` | Puerto del backend |
| `CORS_ALLOWED_ORIGINS` | ❌ | `http://localhost:5173` | Orígenes CORS permitidos |

> ⚠️ = Requerida solo si usas esa integración

---

## 🤝 Contribuir

```bash
# 1. Crea un fork del repositorio
# 2. Crea tu rama de feature
git checkout -b feature/mi-nueva-funcionalidad

# 3. Haz tus cambios y commit
git commit -m "feat: agrega soporte para Twitter API v2"

# 4. Push a tu fork
git push origin feature/mi-nueva-funcionalidad

# 5. Abre un Pull Request
```

### Convención de commits

```
feat:     Nueva funcionalidad
fix:      Corrección de bug
docs:     Solo documentación
style:    Formato, sin cambios de lógica
refactor: Refactorización sin features ni fixes
test:     Agregar o corregir tests
chore:    Tareas de build, dependencias
```

---

## 📄 Licencia

```
MIT License — Copyright (c) 2025 TalentCircle
```

---

<div align="center">

**TalentCircle Pipeline Editorial** · Construido con ❤️ para comunidades técnicas

[⬆ Volver arriba](#-talentcircle--pipeline-editorial-inteligente)

</div>
