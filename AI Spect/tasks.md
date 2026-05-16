# Tareas de Implementación de IA — TalentCircle Content Pipeline

## Visión General

Este documento detalla las tareas necesarias para implementar y mantener el módulo de IA del pipeline. Cubre desde la configuración inicial hasta el testing y monitoreo. Cada tarea referencia requisitos específicos del documento `requirements.md`.

**Estado actual:** ✅ Implementación completa (adapters, servicios, tests, fallback automático, monitoreo de tokens y streaming).

---

## 🚀 Tareas de Implementación

### 1. Configuración de LlmClientPort (Interfaz de Dominio) ✅

**📋 Actividades:**
- Crear interfaz `LlmClientPort` en `domain/port/out/`
- Definir 3 métodos:
  - `AiAnalysis analyzeActivity(List<CommunityActivity>, String promptTemplate)`
  - `String generateDraft(String analysisJson, String channel, String promptTemplate)`
  - `boolean validateConnection(String apiKey)`
- **Requisitos:** RF-AI-01

**✅ Definition of Done:**
- Interfaz creada en el paquete de dominio correcto
- Métodos definidos con tipos del dominio (no tipos de adapter)
- Interfaz importable por servicios de aplicación

---

### 2. Modelo AiAnalysis (Entidad JPA) ✅

**📋 Actividades:**
- Crear entidad `AiAnalysis` en `domain/model/`
- Campos: id (UUID), execution (FK → WeeklyExecution), topTopics (TEXT/JSON), executiveSummary (TEXT), relevanceScores (TEXT/JSON), llmProvider, promptTokens (Integer), completionTokens (Integer)
- Heredar de `AuditableEntity` para createdAt/updatedAt
- Crear `AiAnalysisRepository` extendiendo `JpaRepository<AiAnalysis, String>`
  - Método: `findByExecutionId(String executionId)`
- **Requisitos:** RF-AI-04

**✅ Definition of Done:**
- Entidad JPA creada con anotaciones correctas
- Repositorio funcionando con H2 y PostgreSQL
- findByExecutionId implementado

---

### 3. Modelo PipelineConfig (Configuración de Prompts) ✅

**📋 Actividades:**
- Crear entidad `PipelineConfig` en `domain/model/`
- Campos: llmProvider, llmModel, newsletterPrompt, linkedinPrompt, twitterPrompt, maxItemsPerChannel (Integer, default 10), scheduleCron (default `0 18 ? * FRI`)
- Crear `PipelineConfigRepository` con método `findSingleton()` para acceso al registro único
- **Requisitos:** RF-AI-05, RF-AI-09

**✅ Definition of Done:**
- Entidad creada y mapeada a tabla `pipeline_configs`
- Repositorio con findSingleton
- Seed data para configuración por defecto en migración Flyway

---

### 4. Adaptador OpenAI ✅

**📋 Actividades:**
- Crear `OpenAiClientAdapter` en `adapter/out/llm/`
- Implementar `LlmClientPort`
- Usar `RestTemplate` para llamadas HTTP a `https://api.openai.com/v1/chat/completions`
- Configurar:
  - `@Value("${app.llm.openai.api-key}")`
  - `@Value("${app.llm.openai.model:gpt-4o-mini}")`
- Incluir system prompt fijo en español
- Parámetros: `temperature=0.7`, `max_tokens=4096`
- Anotar con `@Component` y `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai", matchIfMissing = true)`
- **Requisitos:** RF-AI-01, RF-AI-02, RF-AI-12

**✅ Definition of Done:**
- Adapter funcional con OpenAI API
- Provider selection funcionando
- System prompt en español incluido
- Tests pasando con Mockito

---

### 5. Adaptador Claude ✅

**📋 Actividades:**
- Crear `ClaudeClientAdapter` en `adapter/out/llm/`
- Implementar `LlmClientPort`
- Usar `RestTemplate` para `https://api.anthropic.com/v1/messages`
- Headers: `x-api-key`, `anthropic-version: 2023-06-01`
- Anotar con `@Component` y `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")`
- **Requisitos:** RF-AI-01, RF-AI-02

**✅ Definition of Done:**
- Adapter funcional con Anthropic API
- Provider selection condicional
- Tests unitarios

---

### 6. Servicio AiAnalyzerService ✅

**📋 Actividades:**
- Crear `AiAnalyzerService` en `application/service/`
- Implementar `AiAnalyzerUseCase` (input port)
- Flujo:
  1. Obtener `WeeklyExecution` por ID
  2. Obtener `CommunityActivity` de la ejecución
  3. Llamar a `LlmClientPort.analyzeActivity(activities, promptTemplate)`
  4. Vincular `AiAnalysis` a la execution
  5. Persistir `AiAnalysis`
- **Requisitos:** RF-AI-04

**✅ Definition of Done:**
- Servicio implementado con inyección de dependencias correcta
- Análisis persiste en DB
- Tests unitarios pasando con LLM mockeado

---

### 7. Servicio DraftGeneratorService ✅

**📋 Actividades:**
- Crear `DraftGeneratorService` en `application/service/`
- Implementar `DraftGeneratorUseCase`
- Flujo:
  1. Obtener `AiAnalysis` de la ejecución
  2. Convertir analysis a JSON
  3. Generar draft para cada canal (NEWSLETTER, LINKEDIN, TWITTER):
     - Llamar a `LlmClientPort.generateDraft(analysisJson, channel, promptTemplate)`
     - Validar y truncar según límites del canal
     - Asignar `aiScore=0.85` por defecto
     - Asignar `status=PENDING`
  4. Persistir todos los drafts
- Límites: NEWSLETTER=10000, LINKEDIN=3000, TWITTER=280
- **Requisitos:** RF-AI-06, RF-AI-07, RF-AI-08

**✅ Definition of Done:**
- 3 drafts generados por ejecución (uno por canal)
- Límites de caracteres respetados
- Drafts persistidos con status PENDING
- Tests de truncamiento pasando

---

### 8. Sistema de Selección de Proveedor (ConditionalOnProperty) ✅

**📋 Actividades:**
- Agregar `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai", matchIfMissing = true)` en `OpenAiClientAdapter`
- Agregar `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")` en `ClaudeClientAdapter`
- Remover `@Primary` de `OpenAiClientAdapter`
- Verificar que `LlmClientPort` se inyecta correctamente en servicios
- **Requisitos:** RF-AI-02

**✅ Definition of Done:**
- Solamente un adapter activo a la vez
- Default es OpenAI cuando no hay config
- Tests verifican que el adapter correcto se activa

---

### 9. Endpoints de Prueba (OpenAiTestController) ✅

**📋 Actividades:**
- Crear `OpenAiTestController` en `adapter/in/web/`
- Endpoints:
  - `GET /api/v1/test/openai/ping` — validar conexión
  - `POST /api/v1/test/openai/chat` — chat de prueba
  - `POST /api/v1/test/openai/generate` — generar draft de prueba
- Configurar SecurityConfig para permitir `/api/v1/test/**` sin autenticación
- **Requisitos:** RF-AI-10

**✅ Definition of Done:**
- Endpoints públicos funcionando
- Integración verificable manualmente via Swagger o curl
- Endpoints protegidos en producción (solo dev profile)

---

### 10. Pipeline Orchestrator — Integración IA ✅

**📋 Actividades:**
- Integrar `AiAnalyzerUseCase` y `DraftGeneratorUseCase` en `PipelineOrchestratorService`
- Flujo: collect → analyze (Step 2) → generate (Step 3) → complete
- Manejo de errores: si IA falla, marcar execution como FAILED pero preservar datos recolectados
- Método `retryFailedStep` para reintentar desde el pipeline completo
- **Requisitos:** RF-AI-04, RF-AI-06, RNF-AI-02

**✅ Definition of Done:**
- Pipeline orquesta análisis + generación correctamente
- Errores de IA no pierden datos recolectados
- Reintento de pipeline funciona

---

### 11. Configuración de Perfiles (application-dev.yml / application-prod.yml) ✅

**📋 Actividades:**
- Configurar sección `app.llm` en `application-dev.yml`:
  - Provider desde variable de entorno con default `openai`
  - OpenAI key desde `OPENAI_API_KEY` con default vacío
  - Modelo default `gpt-4o-mini`
  - Anthropic key desde `ANTHROPIC_API_KEY` con default vacío
  - Modelo default `claude-3-sonnet-20240229`
- Configurar sección `app.llm` en `application-prod.yml`:
  - Mismos campos pero sin defaults (obligatorios)
  - Modelo `gpt-4-turbo` para producción
- **Requisitos:** RF-AI-13, RF-AI-14

**✅ Definition of Done:**
- Perfiles configurados con valores apropiados para cada entorno
- Variables de entorno documentadas en AGENTS.md

---

### 12. Tests Unitarios ✅

**📋 Actividades:**
- Escribir tests para `AiAnalyzerService`:
  - analyzeActivity con LLM mock → verificar persistencia
  - analyzeActivity sin actividades → comportamiento esperado
- Escribir tests para `DraftGeneratorService`:
  - generateDrafts → 3 drafts creados
  - Twitter draft truncado a 280 chars
  - Newsletter/LinkedIn límites respetados
- Escribir tests para `OpenAiClientAdapter`:
  - validateConnection con key inválida → false
  - generateDraft con placeholder → RuntimeException
- Escribir property tests:
  - Twitter nunca excede 280 caracteres
- **Requisitos:** RF-AI-06 a RF-AI-08, RF-AI-11

**✅ Definition of Done:**
- Tests unitarios cubriendo casos principales y edge cases
- Property tests validando invariantes
- 100% de los tests pasando

---

### 13. ✅ Monitoreo de Tokens y Costos

**📋 Actividades:**
- Implementar contador real de tokens en adaptadores
- Mostrar consumo de tokens en el detalle de ejecución
- (BACKLOG) Agregar alerta cuando consumo supere umbral configurable
- **Requisitos:** RF-AI-11

**✅ Definition of Done:**
- Tokens reales registrados desde respuesta de API: `OpenAiClientAdapter` extrae `prompt_tokens` y `completion_tokens` del campo `usage` en la respuesta de OpenAI. `ClaudeClientAdapter` extrae `input_tokens` y `output_tokens` de la respuesta de Anthropic.
- Dashboard muestra consumo por ejecución (pendiente frontend)
- Alerta configurable (BACKLOG)

---

### 14. ✅ Fallback Automático entre Proveedores

**📋 Actividades:**
- Implementar `FallbackLlmClientAdapter` con `@Primary` que orquesta ambos adaptadores
- El provider activo se define via `app.llm.provider` (default: `openai`)
- Si el primario falla (exception), se intenta con el secundario automáticamente
- Configurar timeouts: connectTimeout=30s, readTimeout=60s via `SimpleClientHttpRequestFactory`
- **Requisitos:** RF-AI-02 (extensión)

**✅ Definition of Done:**
- Fallback automático funcionando en `analyzeActivity`, `generateDraft` y `validateConnection`
- Timeouts configurables (30s connect, 60s read) en ambos adaptadores
- Log de warning al hacer fallback con mensaje del error original
- 11 tests unitarios en `FallbackLlmClientAdapterTest` verificando todos los escenarios

---

### 15. ✅ Streaming de Respuestas LLM (SSE)

**📋 Actividades:**
- Implementar endpoint SSE `GET /api/v1/test/openai/stream` que streamea tokens de OpenAI en tiempo real usando `SseEmitter`
- Configurar `SseEmitter` con timeout de 5 minutos
- Parsear respuesta SSE de OpenAI y emitir eventos `data` individuales por token
- **Requisitos:** UX

**✅ Definition of Done:**
- Endpoint SSE funcional: `GET /api/v1/test/openai/stream?prompt=...`
- Frontend puede consumir eventos SSE para mostrar generación en vivo
- Endpoint protegido por whitelist `/api/v1/test/**` en SecurityConfig

---

## 🔄 Dependencias entre Tareas IA

```
LlmClientPort (1) ─┬─→ OpenAiAdapter (4) ──→ AiAnalyzerService (6) ──┐
                    │                                                 │
                    ├─→ ClaudeAdapter (5) ───→ AiAnalyzerService (6) ─┤
                    │                                                 │
                    └─→ Provider Select (8) ──────────────────────────┤
                                                                      ▼
                                                         PipelineOrchestrator (10)
                                                                      │
                    PipelineConfig (3) ───────────────────────────────┤
                                                                      ▼
                                                         DraftGenerator (7)
                                                                      │
                    AiAnalysis (2) ───────────────────────────────────┘
```

## 📊 Estado por Tarea

| # | Tarea | Estado | Prioridad | Sprint |
|---|-------|--------|-----------|--------|
| 1 | LlmClientPort | ✅ | Alta | Sprint 3 |
| 2 | AiAnalysis Model | ✅ | Alta | Sprint 3 |
| 3 | PipelineConfig Model | ✅ | Alta | Sprint 3 |
| 4 | OpenAI Adapter | ✅ | Alta | Sprint 3 |
| 5 | Claude Adapter | ✅ | Media | Sprint 3 |
| 6 | AiAnalyzerService | ✅ | Alta | Sprint 3 |
| 7 | DraftGeneratorService | ✅ | Alta | Sprint 3 |
| 8 | Provider Selection | ✅ | Alta | Sprint 3 |
| 9 | Test Endpoints | ✅ | Media | Sprint 3 |
| 10 | Pipeline Orchestrator IA | ✅ | Alta | Sprint 3 |
| 11 | Profile Config | ✅ | Alta | Sprint 3 |
| 12 | Unit Tests | ✅ | Alta | Sprint 3 |
| 13 | Token Monitoring | ✅ | Media | Sprint 3 |
| 14 | Provider Fallback | ✅ | Media | Sprint 3 |
| 15 | Streaming | ✅ | Baja | Sprint 3 |

---

## Checklist Final

- [x] Interfaz LlmClientPort definida
- [x] Adaptador OpenAI implementado
- [x] Adaptador Claude implementado
- [x] Provider selection con @ConditionalOnProperty
- [x] AiAnalyzerService funcional
- [x] DraftGeneratorService con límites por canal
- [x] PipelineConfig para prompts en DB
- [x] Endpoints de prueba públicos
- [x] Pipeline Orchestrator integra IA
- [x] Configuración por perfiles (dev/prod)
- [x] Tests unitarios pasando
- [x] Tokens reales desde API LLM
- [x] Fallback automático entre proveedores
- [x] Streaming de generación (SSE)

---

*Plan de Implementación de IA — TalentCircle Content Pipeline v1.0 | Mayo 2026*
