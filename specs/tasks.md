# Plan de Implementación: TalentCircle Content Pipeline

## Visión General

Implementación incremental del backend Spring Boot 3.3 + Java 21 + PostgreSQL + React 18 + TypeScript.
Cada tarea construye sobre la anterior; al final todos los módulos quedan integrados y funcionales.

---

## 🚀 Tareas de Implementación

### 1. Configuración del proyecto base ✅
**📋 Actividades:**

- Actualizar pom.xml: agregar dependencias faltantes (jjwt 0.12, mapstruct, springdoc-openapi 2.x, spring-boot-starter-webflux para LLM)
- Renombrar package de `com.talentcircle` en `TalentCircleApplication.java` y ajustar estructura de carpetas
- Configurar `application.properties` con perfiles dev y prod: datasource, redis, jwt secret/expiry, cors origins, actuator endpoints
- Crear `application-dev.properties` con valores locales y `application-prod.properties` con placeholders de env vars
- Crear `docker-compose.yml` en raíz del proyecto con servicios: postgres:16, redis:7-alpine, y el propio app
- **Requisitos:** RNF-11, RNF-13, RNF-14

**✅ Definition of Done:**
- Proyecto compila sin errores
- Docker compose levanta todos los servicios
- Perfiles dev/prod configurados
- Dependencias críticas agregadas

---

### 2. Infraestructura común ✅
**📋 Actividades:**

**2.1** Crear `AuditableEntity` base con campos `createdAt`, `updatedAt` usando `@MappedSuperclass` y `@EntityListeners(AuditingEntityListener.class)`
- Habilitar JPA Auditing con `@EnableJpaAuditing` en clase de configuración
- **Requisitos:** RNF-06

**2.2** Implementar `UserContext` (ThreadLocal) y `JwtAuthFilter` (OncePerRequestFilter)
- El filtro extrae `userId` y `role` del JWT y los inyecta en el contexto antes de llegar al controlador
- **Requisitos:** RF-28, RF-29

**2.3** Implementar `GlobalExceptionHandler` con `@RestControllerAdvice`
- Manejar: `MethodArgumentNotValidException` → 400, `ResourceNotFoundException` → 404, `DuplicateKeyException` → 409, `AccessDeniedException` → 403, `AuthenticationException` → 401, Exception genérica → 500
- Formato de respuesta: `{ "error": string, "message": string, "timestamp": ISO8601 }`
- **Requisitos:** RNF-09

**2.4** Configurar `SecurityConfig`: deshabilitar CSRF, stateless session, rutas públicas (`/api/v1/auth/**`, `/webhooks/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`), agregar `JwtAuthFilter`
- **Requisitos:** RF-28, RF-29, RNF-07

**2.5** Implementar `EncryptionService` con AES-256 para cifrar/descifrar credenciales de integraciones externas
- **Requisitos:** RNF-08

**2.6** Configurar `WebSocketConfig` con STOMP broker sobre `/ws`, destination prefix `/app`, broker prefix `/topic` y `/queue`
- **Requisitos:** US-08

**✅ Definition of Done:**
- Auditoría automática funcionando
- Filtro de JWT inyecta contexto correctamente
- Manejo de errores unificado
- Security config bloquea acceso no autorizado
- Encryption service pasa tests de encriptación/desencriptación
- WebSockets configurados con STOMP

---

### 3. Módulo Auth ✅
**📋 Actividades:**

**3.1** Crear entidades `User` y `WeeklyExecution` con sus repositorios JPA
- `User`: id (UUID), email, passwordHash, fullName, role (enum ADMIN/EDITOR), active, createdAt, updatedAt
- `WeeklyExecution`: id (UUID), weekStart, weekEnd, status, startedAt, completedAt, triggeredBy
- **Requisitos:** RF-28, RF-30

**3.2** Implementar `JwtService`: generar Access_Token (8 horas), validar tokens, extraer claims
- Claims del token: sub (userId), role, iat, exp
- **Requisitos:** RF-28

**3.3** Implementar `AuthService` con métodos: `login`, `refresh`, `logout`
- `login`: verificar credenciales, emitir tokens, guardar hash del refresh token
- `refresh`: validar refresh token, emitir nuevo par
- `logout`: revocar refresh token del usuario
- **Requisitos:** RF-28

**3.4** Crear `AuthController` con endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- **Requisitos:** RF-28

**3.5** Escribir test de propiedad: round-trip de claims JWT
- **Propiedad 1:** `decode(generate(user)).role == user.role`
- **Valida:** Requisito RF-28

**3.6** Escribir tests unitarios para `AuthService` y `JwtService`
- Casos: login con credenciales inválidas → 401, refresh con token revocado, logout idempotente
- **Requisitos:** RF-28

**✅ Definition of Done:**
- Todos los endpoints auth funcionando
- Tests de propiedad pasando
- 90%+ coverage en servicios auth
- Refresh token mechanism seguro

---

### 4. Checkpoint — Verificar que el contexto de seguridad funciona end-to-end ✅

**📋 Actividades:**
- Asegurar que todos los tests pasan
- Verificar flujo completo: login → refresh → logout
- Validar que JWT tokens funcionan correctamente
- Confirmar que roles ADMIN/EDITOR están operativos

**✅ Checkpoint Validation:**
- Auth flow completo funciona
- JWT tokens validados correctamente
- Error handling unificado funcionando

---

### 5. Módulo Community Collector (Recolección) ✅

**📋 Actividades:**

**5.1** Crear entidades `CommunityActivity`, `CommunitySource` con sus repositorios
- `CommunityActivity`: id, executionId, type (POST/QUESTION/RESOURCE), sourceId, title, content, reactionCount, responseCount, shareCount, author, publishedAt, sourceUrl
- `CommunitySource`: id, name, type (DISCORD/CIRCLE/SLACK), apiUrl, apiKeyEncrypted, active, config (JSONB)
- **Requisitos:** RF-01 a RF-06

**5.2** Implementar `CommunityCollectorService` con métodos: `collectActivity(executionId, sourceId)`
- Obtener posts más reaccionados, preguntas más respondidas, recursos más compartidos
- Persistir en `community_activities` antes del procesamiento IA
- **Requisitos:** RF-01 a RF-06

**5.3** Crear adaptadores para APIs de comunidad: `DiscordClientAdapter`, `CircleClientAdapter`, `SlackClientAdapter`
- Implementar interfaz `CommunityClientPort`
- **Requisitos:** RF-05

**5.4** Crear `CommunityCollectorController` (para testing manual) y configurar `PipelineScheduler`
- Scheduler ejecuta cada viernes 18:00 usando cron configurado
- **Requisitos:** RF-01

**5.5** Escribir test de propiedad: invariante de persistencia
- **Propiedad 2:** `execution.activities.size() > 0 → execution.analysis != null`
- **Valida:** Requisito RF-06

**5.6** Escribir tests unitarios para `CommunityCollectorService`
- Casos: API falla → continúa con otras fuentes, sin actividades → no falla
- **Requisitos:** RF-01 a RF-06

**✅ Definition of Done:**
- Recolección automática funciona
- Soporte para múltiples fuentes (Discord, Circle, Slack)
- Datos persistidos antes de procesamiento IA
- Scheduler configurado para viernes 18:00

---

### 6. Módulo AI Analyzer (Análisis IA) ✅

**📋 Actividades:**

**6.1** Crear entidad `AiAnalysis` con su repositorio
- `AiAnalysis`: id, executionId, topTopics (JSONB), executiveSummary, relevanceScores (JSONB), llmProvider, promptTokens, completionTokens
- **Requisitos:** RF-07 a RF-10

**6.2** Implementar `LlmClientPort` (interfaz) y adaptadores: `OpenAiClientAdapter`, `ClaudeClientAdapter`
- Usar WebClient para llamadas HTTP al LLM API
- **Requisitos:** RF-07, RF-11

**6.3** Implementar `AiAnalyzerService` con métodos: `analyzeActivity(executionId)`
- Asignar puntuación de relevancia 1-10 a cada actividad
- Seleccionar 3-5 temas más relevantes
- Generar resumen ejecutivo en español
- Usar prompts configurables desde `PipelineConfig`
- **Requisitos:** RF-07 a RF-10

**6.4** Crear entidad `PipelineConfig` (singleton) para configuración de prompts
- `PipelineConfig`: id, llmProvider, llmModel, newsletterPrompt, linkedinPrompt, twitterPrompt, maxItemsPerChannel, scheduleCron
- **Requisitos:** RF-10

**6.5** Escribir test de propiedad: round-trip de análisis
- Verificar que el análisis generado corresponde a las actividades
- **Valida:** Requisitos RF-07 a RF-09

**6.6** Escribir tests unitarios para `AiAnalyzerService` y `LlmClientAdapter`
- Casos: LLM no responde → error registrado, prompts dinámicos funcionan
- **Requisitos:** RF-07 a RF-11

**✅ Definition of Done:**
- Análisis IA funcionando con OpenAI y Claude
- Puntuación de relevancia asignada
- Temas seleccionados y resumen ejecutivo generado
- Prompts configurables por canal

---

### 7. Módulo Draft Generator (Generación de Borradores) ✅

**📋 Actividades:**

**7.1** Crear entidad `Draft`, `DraftVersion`, `DraftSource` con sus repositorios
- `Draft`: id, executionId, channel (NEWSLETTER/LINKEDIN/TWITTER), content, editedContent, status (PENDING/APPROVED/REJECTED/PUBLISHED), aiScore, approvedBy, approvedAt, rejectionReason
- `DraftVersion`: id, draftId, content, editedBy, editedAt, versionNumber
- `DraftSource`: id, draftId, activityId, relevanceScore
- **Requisitos:** RF-12 a RF-16

**7.2** Implementar `DraftGeneratorService` con métodos: `generateDrafts(executionId)`
- Generar borrador Newsletter (800-1200 palabras)
- Generar borrador LinkedIn (150-300 palabras)
- Generar borrador Twitter (máximo 280 caracteres)
- Vincular borradores con actividades fuente
- **Requisitos:** RF-12 a RF-15

**7.3** Escribir test de propiedad: metamórfica de canales
- **Propiedad 5:** `draft.channel == TWITTER → draft.content.length() <= 280`
- **Valida:** Requisito RF-14

**7.4** Escribir test de propiedad: monotonicidad de versiones
- **Propiedad 6:** Versiones siempre crecientes en número y timestamp
- **Valida:** Requisito RF-16

**7.5** Escribir tests unitarios para `DraftGeneratorService`
- Casos: Twitter excede 280 chars → truncar, contenido en español
- **Requisitos:** RF-12 a RF-16

**✅ Definition of Done:**
- Borradores generados para los 3 canales
- Límites de caracteres respetados (Twitter 280)
- Vinculación con actividades fuente
- Versiones de borradores funcionando

---

### 8. Checkpoint — Verificar módulos Auth + Collector + AI + Drafts ✅

**📋 Actividades:**
- Asegurar que todos los tests pasan
- Verificar flujo completo: recolección → análisis → generación
- Validar que borradores se crean correctamente
- Confirmar integración con LLM

---

### 9. Módulo Draft Review (Panel Editorial) ✅

**📋 Actividades:**

**9.1** Implementar `DraftReviewService` con métodos: `listDrafts`, `getDraftDetail`, `updateContent`, `approveDraft`, `rejectDraft`
- Listar con filtros: channel, status, weekStart, weekEnd
- Edición inline con guardado automático de versiones
- Aprobación/rechazo con cambio de estado
- **Requisitos:** RF-17 a RF-22

**9.2** Crear `DraftController` con endpoints:
- `GET /api/v1/drafts` (listar con filtros y paginación)
- `GET /api/v1/drafts/{id}` (detalle con fuentes y versiones)
- `PATCH /api/v1/drafts/{id}/content` (editar contenido)
- `POST /api/v1/drafts/{id}/approve` (aprobar)
- `POST /api/v1/drafts/{id}/reject` (rechazar con comentario)
- **Requisitos:** RF-17 a RF-22

**9.3** Escribir test de propiedad: invariante de estado
- **Propiedad 4:** `draft.status == PUBLISHED → draft.publishedAt != null && draft.externalPostId != null`
- **Valida:** Requisito RF-26

**9.4** Escribir tests unitarios para `DraftReviewService`
- Casos: editar borrador → nueva versión creada, aprobar borrador PENDING → APPROVED
- **Requisitos:** RF-17 a RF-22

**✅ Definition of Done:**
- Panel editorial funcionando (listar, ver detalle)
- Edición inline con historial de versiones
- Flujo de aprobación/rechazo operativo
- Filtros y búsqueda implementados

---

### 10. Módulo Publication (Publicación y Exportación) ✅

**📋 Actividades:**

**10.1** Crear entidad `Publication` con su repositorio
- `Publication`: id, draftId, channel, status (SUCCESS/FAILED/RETRYING), externalPostId, publishedAt, errorMessage, retryCount
- **Requisitos:** RF-23 a RF-27

**10.2** Implementar `LinkedInClientAdapter` para LinkedIn API v2
- Publicar post usando endpoint `POST /ugcPosts`
- Manejar reintentos con backoff exponencial (máximo 3)
- **Requisitos:** RF-23, RF-27

**10.3** Implementar `PublicationService` con métodos: `publishDraft`, `exportJson`, `exportCsv`
- Publicar en LinkedIn para borradores APROVED
- Exportar borradores APROVED como JSON o CSV
- **Requisitos:** RF-23 a RF-26

**10.4** Crear endpoints en `DraftController`:
- `POST /api/v1/drafts/{id}/publish` (publicar)
- `GET /api/v1/drafts/export?format=json|csv` (exportar)
- **Requisitos:** RF-23 a RF-26

**10.5** Escribir test de propiedad: idempotencia de publicación
- **Propiedad 8:** Publicar borrador ya publicado retorna mismo `external_post_id`
- **Valida:** Requisito RF-23

**10.6** Escribir tests unitarios para `PublicationService` y `LinkedInClientAdapter`
- Casos: LinkedIn API falla → reintentos, exportación JSON/CSV
- **Requisitos:** RF-23 a RF-27

**✅ Definition of Done:**
- Publicación en LinkedIn funcionando
- Exportación JSON y CSV operativa
- Reintentos con backoff exponencial implementados
- Estados de publicación registrados

---

### 11. Módulo Admin (Administración) ✅

**📋 Actividades:**

**11.1** Implementar `AdminService` con métodos: `manageSources`, `updateConfig`, `manageUsers`, `triggerPipeline`, `listExecutions`
- Gestionar fuentes comunitarias (CRUD)
- Actualizar configuración del pipeline (prompts, LLM provider)
- Gestionar usuarios (CRUD con asignación de roles)
- Ejecutar pipeline manualmente
- Listar historial de ejecuciones
- **Requisitos:** RF-28 a RF-33

**11.2** Crear `AdminController` con endpoints:
- `GET/POST/PUT/DELETE /api/v1/admin/sources` (gestión de fuentes)
- `GET/PUT /api/v1/admin/config` (configuración)
- `GET/POST/PATCH /api/v1/admin/users` (gestión de usuarios)
- `GET/POST /api/v1/executions` (ejecuciones)
- `POST /api/v1/executions/trigger` (ejecución manual)
- **Requisitos:** RF-28 a RF-33

**11.3** Escribir test de propiedad: round-trip de ejecución
- Verificar que ejecuciones manuales y automáticas funcionan igual
- **Valida:** Requisito RF-32

**11.4** Escribir tests unitarios para `AdminService`
- Casos: crear fuente con credenciales encriptadas, cambiar rol manteniendo al menos un ADMIN
- **Requisitos:** RF-28 a RF-33

**✅ Definition of Done:**
- Panel de administración completo
- Gestión de fuentes, usuarios y configuración operativa
- Ejecución manual del pipeline funcionando
- Historial de ejecuciones visible

---

### 12. Pipeline Orchestrator (Orquestación) ✅

**📋 Actividades:**

**12.1** Implementar `PipelineOrchestratorService` que coordina: recolección → análisis → generación
- Emitir eventos: `PipelineStartedEvent`, `ActivityCollectedEvent`, `AnalysisCompletedEvent`, `DraftsGeneratedEvent`
- **Requisitos:** RF-01, RF-06, RF-09, RF-12

**12.2** Configurar Spring Batch Job: `pipelineJob` con steps: `collectStep`, `analyzeStep`, `generateStep`
- Usar Quartz para persistencia de jobs
- **Requisitos:** RF-01

**12.3** Implementar manejo de errores y notificaciones
- Si LLM falla → notificar ADMIN, preservar datos recolectados
- Al completar borradores → notificar EDITOR (email/Slack)
- **Requisitos:** RNF-05, US-08

**12.4** Escribir tests de integración para el flujo completo del pipeline
- Mock de LLM API, verificar estados de `WeeklyExecution`
- **Requisitos:** RF-01 a RF-16

**✅ Definition of Done:**
- Pipeline completo automatizado
- Eventos de dominio funcionando
- Notificaciones de borradores listos
- Manejo de errores robusto

---

### 13. Frontend React + TypeScript ✅

**📋 Actividades:**

**13.1** Configurar proyecto React 18 + TypeScript con Vite
- Instalar TailwindCSS, shadcn/ui, Zustand, React Query, React Router
- **Requisitos:** RNF-15, RNF-16

**13.2** Implementar estructura de carpetas siguiendo el diseño:
- `api/` (clientes Axios), `components/`, `pages/`, `store/`, `hooks/`, `types/`, `utils/`, `router/`
- **Requisitos:** RNF-15

**13.3** Crear páginas principales:
- `Login/` (autenticación)
- `Dashboard/` (resumen de la semana)
- `Drafts/` (lista y detalle/editor)
- `Executions/` (historial de ejecuciones)
- `Admin/` (configuración, solo ADMIN)
- **Requisitos:** RF-17 a RF-22, RF-28 a RF-33

**13.4** Implementar estado global con Zustand y caché con React Query
- Manejar JWT token, usuario autenticado, borradores
- **Requisitos:** RF-28

**13.5** Configurar WebSocket client para notificaciones en tiempo real
- Suscribirse a `/topic/drafts/ready`, `/user/{userId}/queue/notifications`
- **Requisitos:** US-08

**✅ Definition of Done:**
- Frontend funcional y responsivo (1280px+)
- Autenticación JWT integrada
- Panel editorial operativo
- Notificaciones WebSocket funcionando

---

### 14. Migraciones Flyway ✅

**📋 Actividades:**

**14.1** Crear `V1__h2__create_users_and_sources.sql`
- Tablas: `users`, `community_sources`
- Índices: `users(email)`, `community_sources(active)`
- **Requisitos:** RF-28, RF-31

**14.2** Crear `V2__h2__create_weekly_executions.sql`
- Tablas: `weekly_executions`
- Índices: `weekly_executions(week_start, status)`
- **Requisitos:** RF-01

**14.3** Crear `V3__h2__create_community_activities.sql`
- Tablas: `community_activities`
- Índices: `community_activities(execution_id, type)`
- **Requisitos:** RF-02 a RF-06

**14.4** Crear `V4__h2__create_ai_analyses.sql`
- Tablas: `ai_analyses`
- **Requisitos:** RF-07 a RF-10

**14.5** Crear `V5__h2__create_drafts_and_versions.sql`
- Tablas: `drafts`, `draft_versions`, `draft_sources`
- Índices: `drafts(execution_id, channel, status)`
- **Requisitos:** RF-12 a RF-16, RF-20

**14.6** Crear `V6__h2__create_publications.sql`
- Tablas: `publications`
- **Requisitos:** RF-23 a RF-27

**14.7** Crear `V7__h2__create_pipeline_config.sql`
- Tablas: `pipeline_configs` (singleton)
- **Requisitos:** RF-10

**14.8** Crear `V8__h2__seed_default_config.sql`
- Insertar configuración por defecto con prompts base para cada canal
- **Requisitos:** RF-10

**✅ Definition of Done:**
- Todas las migraciones creadas y probadas
- Índices optimizados para consultas frecuentes
- Datos semilla insertados correctamente

---

### 15. Tests de Integración ✅

**📋 Actividades:**

**15.1** Escribir tests de integración para el flujo completo de Auth
- Usar `@SpringBootTest` + Testcontainers (PostgreSQL + Redis)
- Flujo: login → refresh → logout → intentar refresh con token revocado
- **Requisitos:** RF-28

**15.2** Escribir tests de integración para el pipeline completo
- Simular ejecución: recolección → análisis IA → generación de borradores
- Verificar estados de `WeeklyExecution` y creación de borradores
- **Requisitos:** RF-01 a RF-16

**15.3** Escribir tests de integración para el panel editorial
- Crear borrador → editar → aprobar → publicar en LinkedIn
- Verificar cambios de estado y creación de `Publication`
- **Requisitos:** RF-17 a RF-26

**15.4** Escribir tests de propiedad para validación de invariantes
- Propiedades 1-8 definidas en el documento de requisitos
- **Requisitos:** Todos

**✅ Definition of Done:**
- Tests de integración pasando
- Cobertura de tests >= 80%
- Propiedades de corrección validadas

---

### 16. Configuración Final y Observabilidad ✅

**📋 Actividades:**

**16.1** Configurar logging estructurado JSON con Logback
- Incluir en cada log: método, path, statusCode, duración ms, userId
- **Requisitos:** RNF-10

**16.2** Configurar rate limiting en endpoints de auth (`/api/v1/auth/**`) usando `bucket4j` + Redis
- 10 req/s por IP
- **Requisitos:** RNF-07

**16.3** Configurar SpringDoc OpenAPI
- Habilitar solo en perfil `dev`, configurar `SecurityScheme` para JWT Bearer
- **Requisitos:** RNF-13

**16.4** Verificar endpoint `/actuator/health` con health indicators para PostgreSQL y Redis
- **Requisitos:** RNF-09

**16.5** Actualizar `docker-compose.yml` con configuración completa
- Variables de entorno, volúmenes persistentes para postgres, healthchecks, red interna
- **Requisitos:** RNF-11

**16.6** Configurar métricas Prometheus y trazas Jaeger
- Exponer `/actuator/prometheus`
- **Requisitos:** RNF-14

**✅ Definition of Done:**
- Logging estructurado funcionando
- Rate limiting activo
- API documentada con Swagger UI
- Health checks operativos
- Docker Compose completo y funcional
- Métricas y trazas disponibles

---

### 17. Checkpoint Final — Verificar Sistema Completo ✅

**📋 Actividades:**
- Ejecutar suite completa de tests unitarios e integración
- Verificar que todos los requisitos están implementados
- Asegurar cobertura de tests >= 80%
- Validar propiedades de corrección
- Consultar al usuario si hay dudas antes de hacer merge

**✅ Checkpoint Validation:**
- Todos los tests pasando
- Requisitos funcionales y no funcionales cumplidos
- Sistema listo para despliegue

---

## Cronograma de Sprints (14 semanas)

| Sprint | Fechas | Objetivos | Módulos |
|--------|---------|-----------|---------|
| **Sprint 0** | Semana 1 | Setup y Arquitectura Base | Tarea 1, 2 |
| **Sprint 1** | Semanas 2-3 | Autenticación y CRUD Fuentes | Tarea 3, 5 |
| **Sprint 2** | Semanas 4-5 | Pipeline de Recolección | Tarea 5 |
| **Sprint 3** | Semanas 6-7 | Análisis IA y Generación | Tarea 6, 7 |
| **Sprint 4** | Semanas 8-9 | Panel Editorial | Tarea 9 |
| **Sprint 5** | Semanas 10-11 | Publicación y Exportación | Tarea 10 |
| **Sprint 6** | Semanas 12-13 | Admin Panel y Observabilidad | Tarea 11, 16 |
| **Sprint 7** | Semana 14 | Testing, Hardening y Demo | Tarea 12, 13, 14, 15, 17 |

---

## 🔄 Dependencias Críticas

| Módulo | Depende de | Bloquea a |
|--------|------------|-----------|
| **Auth** | Infraestructura común | Todos los módulos |
| **Community Collector** | Auth | AI Analyzer |
| **AI Analyzer** | Community Collector, LLM Config | Draft Generator |
| **Draft Generator** | AI Analyzer | Draft Review |
| **Draft Review** | Draft Generator | Publication |
| **Publication** | Draft Review, LinkedIn Config | Ninguno |
| **Admin** | Auth | Ninguno (mejora) |
| **Frontend** | Todos los APIs | Ninguno (interfaz) |
| **Migraciones** | Todos los módulos | Tests de integración |
| **Observabilidad** | Todos los módulos | Despliegue |

---

## 📞 Puntos de Atención

1. **⚠️ LLM Costs**: Monitorear tokens usados, configurar límites en `PipelineConfig`
2. **⚠️ LinkedIn API**: Verificar límites de rate y políticas de contenido
3. **⚠️ Múltiples Fuentes**: Implementar una fuente a la vez, probar bien antes de agregar otra
4. **⚠️ WebSocket**: Configurar correctamente para notificaciones en tiempo real
5. **⚠️ Docker**: Asegurar que todas las variables de entorno estén documentadas

---

## ✅ Checklist Final Pre-Producción

- [x] Todos los módulos implementados
- [x] Tests de propiedad completos (8/8)
- [x] Tests unitarios >80% coverage
- [x] Tests de integración completos
- [x] Logging estructurado implementado
- [x] API documentada con OpenAPI
- [ ] Performance validada (RNF-01 a RNF-03)
- [ ] Security audit completada (RNF-07 a RNF-10)
- [x] Docker Compose listo para producción
- [x] Frontend responsivo y accesible (RNF-15 a RNF-17)
- [x] Migraciones Flyway ejecutadas exitosamente
- [ ] Pipeline completo probado end-to-end

---

## Notas

- Las tareas marcadas con `⏳` están pendientes por implementar
- Las tareas marcadas con `✅` están completadas
- Cada tarea referencia requisitos específicos para trazabilidad
- Los checkpoints garantizan validación incremental antes de avanzar al siguiente módulo
- Los tests de propiedad validan invariantes universales; los tests unitarios validan casos concretos y edge cases
- El orden de las tareas respeta dependencias: infraestructura → auth → entidades → servicios → controladores → migraciones → tests de integración
- El frontend se desarrolla en paralelo una vez que los endpoints principales están estables

---

*Plan de Implementación - TalentCircle Content Pipeline v1.0 | Abril 2025*
