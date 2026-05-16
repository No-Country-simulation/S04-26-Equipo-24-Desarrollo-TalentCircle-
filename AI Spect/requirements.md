# Requisitos de IA — TalentCircle Content Pipeline

## Introducción

Módulo de inteligencia artificial del pipeline de contenido comunitario automatizado para TalentCircle. Proporciona análisis de actividad comunitaria y generación de borradores editoriales usando LLMs (OpenAI y Anthropic Claude). Sigue arquitectura hexagonal con el puerto `LlmClientPort` como interfaz de dominio.

---

## Glosario

- **System**: El backend Spring Boot del pipeline TalentCircle
- **LlmClientPort**: Puerto de salida (output port) que abstrae el proveedor LLM
- **OpenAiClientAdapter**: Adaptador para OpenAI GPT
- **ClaudeClientAdapter**: Adaptador para Anthropic Claude
- **AiAnalyzerService**: Servicio que analiza actividad comunitaria usando LLM
- **DraftGeneratorService**: Servicio que genera borradores por canal usando LLM
- **AiAnalysis**: Resultado del análisis de IA (topics, summary, scores, tokens)
- **CommunityActivity**: Actividad recolectada de la comunidad que alimenta el análisis
- **Draft**: Borrador de contenido generado por IA para un canal
- **PipelineConfig**: Configuración del pipeline incluyendo prompts del LLM
- **Channel**: Destino de publicación (NEWSLETTER, LINKEDIN, TWITTER)
- **Prompt Template**: Plantilla de texto enviada al LLM para guiar la generación
- **Token**: Unidad de medición de uso de API del LLM
- **Editor**: Usuario con rol EDITOR que revisa borradores generados por IA

---

## Requisitos Funcionales

### Módulo: Integración con LLM

#### RF-AI-01: Abstracción de Proveedor LLM
**Historia de Usuario:** Como sistema, quiero una interfaz única para interactuar con diferentes proveedores LLM, para poder cambiar de proveedor sin modificar el código de negocio.

**Criterios de Aceptación**
1. THE system SHALL definir una interfaz `LlmClientPort` en el paquete de dominio.
2. THE interfaz SHALL declarar métodos: `analyzeActivity`, `generateDraft`, `validateConnection`.
3. THE adapters (`OpenAiClientAdapter`, `ClaudeClientAdapter`) SHALL implementar esta interfaz.
4. THE servicios de aplicación (`AiAnalyzerService`, `DraftGeneratorService`) SHALL depender solo de la interfaz, no de implementaciones concretas.

#### RF-AI-02: Selección de Proveedor por Configuración
**Historia de Usuario:** Como administrador, quiero configurar qué proveedor LLM usar mediante variable de entorno, para elegir según costo y calidad.

**Criterios de Aceptación**
1. WHEN `LLM_PROVIDER=openai` o no está configurado, THE system SHALL usar `OpenAiClientAdapter`.
2. WHEN `LLM_PROVIDER=anthropic`, THE system SHALL usar `ClaudeClientAdapter`.
3. THE selección SHALL usar `@ConditionalOnProperty` de Spring Boot.
4. IF el proveedor configurado no tiene API key, THE system SHALL lanzar `IllegalStateException`.

#### RF-AI-03: Validación de Conexión
**Historia de Usuario:** Como administrador, quiero verificar que la API key del LLM funciona antes de ejecutar el pipeline, para evitar errores en producción.

**Criterios de Aceptación**
1. WHEN `validateConnection(apiKey)` es llamado con una key válida, THE system SHALL retornar `true`.
2. WHEN es llamado con una key inválida o vacía, THE system SHALL retornar `false`.
3. THE método SHALL hacer una llamada real a la API del proveedor para verificar.

---

### Módulo: Análisis de Actividad

#### RF-AI-04: Análisis de Actividad Comunitaria
**Historia de Usuario:** Como sistema, quiero analizar las actividades recolectadas usando el LLM, para extraer temas relevantes y resumen ejecutivo.

**Criterios de Aceptación**
1. WHEN `AiAnalyzerService.analyzeActivity(executionId)` es llamado, THE system SHALL obtener las actividades de la ejecución desde `CommunityActivityRepository`.
2. THE system SHALL construir un prompt con las actividades y llamar al LLM vía `LlmClientPort`.
3. THE LLM SHALL retornar un `AiAnalysis` con: `executiveSummary`, `topTopics`, `relevanceScores`.
4. THE system SHALL persistir el `AiAnalysis` en la tabla `ai_analyses`.
5. THE `AiAnalysis` SHALL quedar vinculado a la `WeeklyExecution` correspondiente.
6. THE system SHALL registrar el proveedor LLM usado en `llmProvider`.

#### RF-AI-05: Configuración de Prompts de Análisis
**Historia de Usuario:** Como administrador, quiero personalizar el prompt de análisis que se envía al LLM, para ajustar el enfoque del análisis.

**Criterios de Aceptación**
1. THE prompt template SHALL ser configurable vía `PipelineConfig` en base de datos.
2. THE prompt SHALL soportar las variables: `{activities}`, `{topics}`, `{executive_summary}`.
3. WHEN no hay prompt configurado, THE system SHALL usar el prompt por defecto: `"Analyze these activities for content generation"`.

---

### Módulo: Generación de Borradores

#### RF-AI-06: Generación de Borrador Newsletter
**Historia de Usuario:** Como sistema, quiero generar un borrador de newsletter en español usando el LLM, para el boletín semanal.

**Criterios de Acpetación**
1. WHEN `DraftGeneratorService.generateDrafts(executionId)` es llamado, THE system SHALL generar un draft para canal `NEWSLETTER`.
2. THE LLM SHALL recibir: el análisis JSON + el prompt de newsletter configurado.
3. THE contenido generado SHALL respetar el límite de 10,000 caracteres.
4. THE draft SHALL crearse con `status=PENDING`.
5. THE draft SHALL tener `aiScore=0.85` por defecto.

#### RF-AI-07: Generación de Borrador LinkedIn
**Historia de Usuario:** Como sistema, quiero generar un borrador de LinkedIn en español usando el LLM, para publicación profesional.

**Criterios de Aceptación**
1. THE system SHALL generar un draft para canal `LINKEDIN`.
2. THE contenido generado SHALL respetar el límite de 3,000 caracteres.
3. THE draft SHALL crearse con `status=PENDING`.

#### RF-AI-08: Generación de Borrador Twitter
**Historia de Usuario:** Como sistema, quiero generar un borrador de Twitter en español usando el LLM, máximo 280 caracteres.

**Criterios de Aceptación**
1. THE system SHALL generar un draft para canal `TWITTER`.
2. IF el contenido generado excede 280 caracteres, THE system SHALL truncar a 277 caracteres + "...".
3. THE draft SHALL crearse con `status=PENDING`.

#### RF-AI-09: Prompts Configurables por Canal
**Historia de Usuario:** Como administrador, quiero configurar prompts diferentes para cada canal de publicación, para adaptar tono y formato.

**Criterios de Aceptación**
1. THE system SHALL leer los prompts desde `PipelineConfig.newsletterPrompt`, `PipelineConfig.linkedinPrompt`, `PipelineConfig.twitterPrompt`.
2. SI no hay prompts en DB, THE system SHALL usar defaults de `application.yml`.
3. THE prompts SHALL poder actualizarse via `PUT /api/v1/admin/config`.

---

### Módulo: Testing y Monitoreo

#### RF-AI-10: Endpoints de Prueba
**Historia de Usuario:** Como desarrollador, quiero endpoints públicos para probar la integración con el LLM, para verificar que la API key funciona.

**Criterios de Aceptación**
1. `GET /api/v1/test/openai/ping` SHALL verificar la conexión con el LLM.
2. `POST /api/v1/test/openai/chat` SHALL enviar un mensaje al LLM y retornar la respuesta.
3. `POST /api/v1/test/openai/generate` SHALL probar la generación de drafts.
4. Estos endpoints SHALL ser públicos (sin autenticación).

#### RF-AI-11: Registro de Tokens
**Historia de Usuario:** Como administrador, quiero saber cuántos tokens consume cada ejecución del pipeline, para controlar costos.

**Criterios de Aceptación**
1. THE `AiAnalysis` SHALL almacenar `promptTokens` y `completionTokens`.
2. THE tokens SHALL estar disponibles en el detalle de ejecución.
3. (BACKLOG) THE system SHALL alertar si el consumo de tokens excede un umbral configurable.

#### RF-AI-12: Sistema por Defecto (System Prompt)
**Historia de Usuario:** Como sistema, quiero que el LLM tenga un system prompt fijo que asegure contenido en español y tono apropiado, para consistencia.

**Criterios de Aceptación**
1. THE `OpenAiClientAdapter` SHALL incluir un system message: *"You are a helpful content creator and community manager. Analyze community activities and generate high-quality social media content in SPANISH (Espanol)."*
2. THE `ClaudeClientAdapter` SHALL incluir instrucción equivalente en el prompt.

---

### Requisitos de Infraestructura

#### RF-AI-13: Variables de Entorno
| Variable | Requerida | Default | Descripción |
|----------|-----------|---------|-------------|
| `OPENAI_API_KEY` | Sí (si provider=openai) | - | API key de OpenAI |
| `OPENAI_MODEL` | No | `gpt-4o-mini` | Modelo OpenAI |
| `ANTHROPIC_API_KEY` | Sí (si provider=anthropic) | - | API key de Anthropic |
| `ANTHROPIC_MODEL` | No | `claude-3-sonnet-20240229` | Modelo Claude |
| `LLM_PROVIDER` | No | `openai` | Proveedor activo |

#### RF-AI-14: Perfiles de Configuración
1. **Dev** (`application-dev.yml`): H2 en memoria, modelos ligeros (`gpt-4o-mini`, `claude-3-sonnet`), logging DEBUG.
2. **Prod** (`application-prod.yml`): PostgreSQL, modelos producción (`gpt-4-turbo`), logging INFO.
3. **Docker** (`application-docker.yml`): Config para entorno containerizado.

---

## Requisitos No Funcionales

**RNF-AI-01**: THE timeout para llamadas LLM SHALL ser de 60 segundos.
**RNF-AI-02**: IF el LLM no responde, THE pipeline SHALL registrar el error, marcar la ejecución como FAILED y preservar los datos recolectados.
**RNF-AI-03**: LAS API keys SHALL leerse exclusivamente de variables de entorno, nunca de archivos de configuración en el repositorio.
**RNF-AI-04**: EL sistema SHALL soportar hasta 3 canales de generación por ejecución (NEWSLETTER, LINKEDIN, TWITTER).
**RNF-AI-05**: EL contenido generado SHALL estar en español por defecto.

---

## Propiedades de Corrección (Property-Based Testing)

1. **Round-trip de proveedor**: Para toda configuración `app.llm.provider`, el adapter activo SHALL ser el correspondiente. Formalmente: `provider=openai → OpenAiClientAdapter es @Primary`.
2. **Invariante de canal Twitter**: Para todo draft con `channel=TWITTER`, `content.length() <= 280`. Formalmente: `draft.channel == TWITTER → draft.content.length() <= 280`.
3. **Invariante de análisis**: Para toda ejecución completada con actividades, SHALL existir un `AiAnalysis`. Formalmente: `activities.size() > 0 → analysis != null`.

---

## Actores del Sistema

| Actor | Descripción | Interacción con IA |
|-------|-------------|-------------------|
| Administrador | Configura proveedor LLM, prompts, modelo | Configura vía Admin API |
| Editor | Revisa borradores generados por IA | Aprueba/rechaza contenido generado |
| Sistema LLM (Externo) | API de OpenAI o Anthropic | Recibe prompts, devuelve análisis y drafts |
| Sistema (Pipeline) | Orquesta el flujo IA | Llama a LLM vía LlmClientPort |

---

## Dependencias con Otros Módulos

| Módulo | Dependencia | Dirección |
|--------|-------------|-----------|
| Community Collector | Provee actividades para analizar | → AI Analyzer |
| AI Analyzer | Depende de LlmClientPort + CommunityActivityRepository | Interno |
| Draft Generator | Depende de LlmClientPort + AiAnalysisRepository | Interno |
| Pipeline Orchestrator | Orquesta AI Analyzer + Draft Generator | → Ambos |
| Admin | Configura prompts y proveedor LLM | → PipelineConfig |
| Publication | Consume drafts generados | ← Draft Generator |

---

*Documento de Requisitos de IA — TalentCircle Content Pipeline v1.0 | Mayo 2026*
