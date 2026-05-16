# Documento de Requisitos: TalentCircle Content Pipeline

## Introducción

Pipeline de contenido comunitario automatizado para TalentCircle. Construido con Java21 + Spring Boot 3.x + PostgreSQL + React 18.
Sigue el patrón de microservicios desacoplados con comunicación asíncrona. Este documento cubre los requisitos completos del sistema.

---

## Glosario

- **System**: El backend Spring Boot del pipeline TalentCircle
- **PipelineOrchestrator**: Componente responsable de coordinar la ejecución semanal
- **CommunityCollector**: Componente responsable de recolectar actividad de APIs externas
- **AiAnalyzer**: Componente responsable de análisis con LLM (OpenAI/Claude)
- **DraftGenerator**: Componente responsable de generar borradores por canal
- **DraftReviewService**: Componente responsable del ciclo de vida de borradores
- **PublisherService**: Componente responsable de publicación y exportación
- **AuthService**: Componente responsable de autenticación JWT
- **AdminService**: Componente responsable de configuración del sistema
- **WeeklyExecution**: Representa una ejecución completa del pipeline semanal
- **CommunityActivity**: Actividad recolectada de la comunidad (post, pregunta, recurso)
- **AiAnalysis**: Resultado del análisis de IA sobre la actividad
- **Draft**: Borrador de contenido para un canal específico
- **Publication**: Registro de publicación de un borrador
- **Editor**: Usuario con rol EDITOR que revisa y aprueba borradores
- **Administrator**: Usuario con rol ADMIN que configura el sistema
- **Channel**: Destino de publicación (NEWSLETTER, LINKEDIN, TWITTER)
- **DraftStatus**: Estado del borrador (PENDING, APPROVED, REJECTED, PUBLISHED)

---

## Requisitos Funcionales

### Módulo: Recolección de Actividad Comunitaria

#### Requisito 1: Recolección Automática Semanal (RF-01)
**Historia de Usuario:** Como administrador, quiero que el sistema ejecute automáticamente la recolección cada viernes a las 18:00, para tener el contenido listo sin intervención manual.

**Criterios de Aceptación**
1. WHEN la hora sea viernes 18:00 (hora local configurada), THE system SHALL ejecutar automáticamente la recolección de datos.
2. THE system SHALL usar Spring Scheduler o Quartz para programar la ejecución.
3. IF la ejecución falla, THE system SHALL registrar el error y notificar al ADMIN.
4. THE system SHALL soportar configuración del cron schedule en la base de datos.

#### Requisito 2: Posts Más Reaccionados (RF-02)
**Historia de Usuario:** Como sistema, quiero obtener los N posts con mayor reacciones de la semana, para identificar contenido relevante.

**Criterios de Aceptación**
1. WHEN se ejecuta la recolección, THE CommunityCollector SHALL obtener los N posts con mayor número de reacciones de los últimos 7 días.
2. THE system SHALL soportar configuración de N (default: 10).
3. THE system SHALL almacenar: title, content, author, reactionCount, publishedAt, sourceUrl.
4. IF no hay posts con reacciones, THE system SHALL continuar con el siguiente paso sin fallar.

#### Requisito 3: Preguntas Más Respondidas (RF-03)
**Historia de Usuario:** Como sistema, quiero obtener las preguntas con mayor respuestas, para identificar dudas de la comunidad.

**Criterios de Aceptación**
1. WHEN se ejecuta la recolección, THE CommunityCollector SHALL obtener las preguntas con mayor número de respuestas de los últimos 7 días.
2. THE system SHALL almacenar: title, content, author, responseCount, publishedAt.
3. THE system SHALL clasificar el tipo de actividad como QUESTION.

#### Requisito 4: Recursos Más Compartidos (RF-04)
**Historia de Usuario:** Como sistema, quiero identificar los recursos más compartidos, para amplificar contenido valioso.

**Criterios de Aceptación**
1. WHEN se ejecuta la recolección, THE CommunityCollector SHALL identificar los recursos (links, archivos) con mayor número de comparticiones.
2. THE system SHALL almacenar: title, content (URL), shareCount, author.
3. THE system SHALL clasificar el tipo de actividad como RESOURCE.

#### Requisito 5: Múltiples Fuentes (RF-05)
**Historia de Usuario:** Como administrador, quiero configurar múltiples fuentes de comunidad, para conectar diferentes plataformas.

**Criterios de Aceptación**
1. THE system SHALL soportar configuración de múltiples fuentes: Discord, Circle.so, Slack.
2. WHEN un ADMIN hace POST a `/api/v1/admin/sources`, THE system SHALL crear una nueva fuente con credenciales encriptadas.
3. WHEN un ADMIN hace PUT a `/api/v1/admin/sources/{id}`, THE system SHALL actualizar la configuración de la fuente.
4. WHEN un ADMIN hace DELETE a `/api/v1/admin/sources/{id}`, THE system SHALL desactivar la fuente.

#### Requisito 6: Almacenamiento de Actividad Cruda (RF-06)
**Historia de Usuario:** Como sistema, quiero persistir toda actividad recolectada antes del procesamiento IA, para no perder datos ante fallos.

**Criterios de Aceptación**
1. THE system SHALL persistir toda actividad recolectada en la tabla `community_activities` antes del procesamiento IA.
2. WHEN se completa la recolección, THE system SHALL emitir un `ActivityCollectedEvent` con el executionId y itemCount.
3. IF el procesamiento IA falla, THE system SHALL preservar los datos recolectados para reprocesamiento.

---

### Módulo: Análisis IA

#### Requisito 7: Análisis de Relevancia (RF-07)
**Historia de Usuario:** Como sistema, quiero que el LLM evalúe cada ítem y asigne una puntuación de relevancia del 1 al 10, para priorizar contenido.

**Criterios de Aceptación**
1. WHEN el `ActivityCollectedEvent` es recibido, THE AiAnalyzer SHALL procesar cada ítem con el LLM.
2. THE LLM SHALL asignar una puntuación de relevancia del 1 al 10 a cada actividad.
3. THE system SHALL almacenar el score en la tabla `community_activities` o en `ai_analyses` como JSONB.
4. THE AiAnalyzer SHALL usar el proveedor LLM configurado (OpenAI o Claude).

#### Requisito 8: Selección de Temas (RF-08)
**Historia de Usuario:** Como sistema, quiero identificar los 3-5 temas más relevantes de la semana, para usar como base de los borradores.

**Criterios de Aceptación**
1. WHEN el análisis de relevancia se completa, THE AiAnalyzer SHALL agrupar actividades por temática.
2. THE system SHALL seleccionar los 3-5 temas más relevantes basados en los scores y agrupación.
3. THE system SHALL almacenar los temas en `ai_analyses.top_topics` (JSONB).

#### Requisito 9: Generación de Resumen Ejecutivo (RF-09)
**Historia de Usuario:** Como sistema, quiero generar un resumen ejecutivo de la actividad semanal en español, para dar contexto editorial.

**Criterios de Aceptación**
1. THE AiAnalyzer SHALL generar un resumen ejecutivo en español usando el LLM.
2. THE resumen SHALL cubrir los temas principales y la actividad más destacada.
3. THE system SHALL almacenar el resumen en `ai_analyses.executive_summary`.
4. THE system SHALL registrar tokens usados (prompt_tokens, completion_tokens) para control de costos.

#### Requisito 10: Configuración de Prompts (RF-10)
**Historia de Usuario:** Como administrador, quiero ajustar los prompts del LLM por canal, para personalizar tono y formato.

**Criterios de Aceptación**
1. WHEN un ADMIN hace GET a `/api/v1/admin/config`, THE system SHALL retornar los prompts actuales para cada canal.
2. WHEN un ADMIN hace PUT a `/api/v1/admin/config`, THE system SHALL actualizar los prompts con variables dinámicas.
3. THE system SHALL soportar variables: `{topics}`, `{executive_summary}`, `{activities}`, `{channel}`.
4. THE prompts SHALL estar en la tabla `pipeline_configs` (registro único).

#### Requisito 11: Soporte Multi-LLM (RF-11 - Backlog)
**Historia de Usuario:** Como administrador, quiero configurar el proveedor LLM, para usar OpenAI GPT-4 o Claude Sonnet.

**Criterios de Aceptación**
1. THE system SHALL soportar configuración del proveedor LLM en `pipeline_configs.llm_provider`.
2. THE system SHALL soportar modelos: `gpt-4-turbo`, `claude-3-sonnet-20240229`.
3. WHEN el proveedor configurado no responde, THE system SHALL intentar con el alternativo si está configurado.

---

### Módulo: Generación de Borradores

#### Requisito 12: Borrador Newsletter (RF-12)
**Historia de Usuario:** Como sistema, quiero generar un borrador de newsletter en español, formato largo (800-1200 palabras), para el boletín semanal.

**Criterios de Aceptación**
1. WHEN el `AnalysisCompletedEvent` es recibido, THE DraftGenerator SHALL generar un borrador para Newsletter.
2. THE borrador SHALL tener 800-1200 palabras en español.
3. THE borrador SHALL incluir: título, secciones por tema, CTAs.
4. THE system SHALL almacenar el borrador con `channel=NEWSLETTER` y `status=PENDING`.

#### Requisito 13: Borrador LinkedIn (RF-13)
**Historia de Usuario:** Como sistema, quiero generar un borrador de LinkedIn en español, formato mediano (150-300 palabras), para publicación profesional.

**Criterios de Aceptación**
1. THE DraftGenerator SHALL generar un borrador para LinkedIn con 150-300 palabras.
2. THE borrador SHALL tener tono profesional y emojis moderados.
3. THE system SHALL almacenar el borrador con `channel=LINKEDIN` y `status=PENDING`.

#### Requisito 14: Borrador Twitter/X (RF-14)
**Historia de Usuario:** Como sistema, quiero generar un borrador de Twitter en español, máximo 280 caracteres, para amplificación rápida.

**Criterios de Aceptación**
1. THE DraftGenerator SHALL generar un borrador para Twitter con máximo 280 caracteres.
2. THE borrador SHALL incluir hashtags relevantes.
3. THE system SHALL almacenar el borrador con `channel=TWITTER` y `status=PENDING`.

#### Requisito 15: Vinculación de Fuentes (RF-15)
**Historia de Usuario:** Como editor, quiero ver qué posts/recursos originaron un borrador, para evaluar fidelidad del contenido.

**Criterios de Aceptación**
1. THE DraftGenerator SHALL vincular cada borrador con las actividades que lo originaron.
2. THE system SHALL usar la tabla `draft_sources` como tabla de unión M:N.
3. WHEN un editor ve el detalle del borrador, THE system SHALL mostrar las actividades fuente.

#### Requisito 16: Versiones de Borrador (RF-16)
**Historia de Usuario:** Como editor, quiero que el sistema mantenga versiones de cada borrador tras edición, para historial de cambios.

**Criterios de Aceptación**
1. WHEN un editor edita un borrador, THE system SHALL guardar la versión anterior en `draft_versions`.
2. THE system SHALL almacenar: content, editedBy, editedAt, versionNumber.
3. THE system SHALL permitir ver versiones anteriores en el panel editorial.

---

### Módulo: Panel de Revisión Editorial

#### Requisito 17: Listado de Borradores (RF-17)
**Historia de Usuario:** Como editor, quiero ver todos los borradores de la semana en un panel único, para revisar eficientemente.

**Criterios de Aceptación**
1. WHEN un EDITOR hace GET a `/api/v1/drafts`, THE system SHALL retornar lista paginada de borradores.
2. THE listado SHALL incluir: canal, estado, fecha, resumen del contenido.
3. THE system SHALL soportar paginación con parámetros `page` y `size`.
4. THE system SHALL ordenar por fecha de creación descendente por defecto.

#### Requisito 18: Vista Detalle (RF-18)
**Historia de Usuario:** Como editor, quiero leer el borrador completo con su contexto de origen, para evaluar fidelidad.

**Criterios de Aceptación**
1. WHEN un EDITOR hace GET a `/api/v1/drafts/{id}`, THE system SHALL retornar el borrador con contenido completo.
2. THE system SHALL incluir las actividades fuente y sus scores de relevancia.
3. THE system SHALL incluir el historial de versiones del borrador.
4. IF el borrador no existe o no pertenece al workspace, THE system SHALL retornar HTTP 404.

#### Requisito 19: Edición Inline (RF-19)
**Historia de Usuario:** Como editor, quiero editar el texto del borrador directamente en el panel, para ajustar tono o precisión.

**Criterios de Aceptación**
1. WHEN un EDITOR hace PATCH a `/api/v1/drafts/{id}/content`, THE system SHALL actualizar el contenido.
2. THE system SHALL guardar la versión anterior en `draft_versions`.
3. THE system SHALL soportar autoguardado (debounce del frontend).
4. THE system SHALL actualizar el campo `updated_at` automáticamente.

#### Requisito 20: Flujo de Aprobación (RF-20)
**Historia de Usuario:** Como editor, quiero aprobar o rechazar borradores con un clic, para agilizar el flujo.

**Criterios de Aceptación**
1. WHEN un EDITOR hace POST a `/api/v1/drafts/{id}/approve`, THE system SHALL cambiar el estado a `APPROVED`.
2. WHEN un EDITOR hace POST a `/api/v1/drafts/{id}/reject`, THE system SHALL cambiar el estado a `REJECTED` y requerir comentario.
3. THE system SHALL registrar quién aprobó/rechazó y cuándo (`approved_by`, `approved_at`).
4. IF el borrador ya está aprobado, THE system SHALL retornar HTTP 400.

#### Requisito 21: Filtros y Búsqueda (RF-21)
**Historia de Usuario:** Como editor, quiero filtrar borradores por canal, estado y fecha, para encontrar rápidamente lo que busco.

**Criterios de Aceptación**
1. THE system SHALL soportar filtros: `channel` (NEWSLETTER, LINKEDIN, TWITTER), `status` (PENDING, APPROVED, REJECTED, PUBLISHED).
2. THE system SHALL soportar filtro por rango de fechas: `week_start`, `week_end`.
3. THE system SHALL retornar solo borradores del workspace del usuario autenticado.

#### Requisito 22: Indicador de IA Score (RF-22 - Backlog)
**Historia de Usuario:** Como editor, quiero ver la puntuación de relevancia asignada por el LLM, para priorizar revisión.

**Criterios de Aceptación**
1. THE system SHALL mostrar el `ai_score` en el listado y detalle del borrador.
2. THE system SHALL permitir ordenar borradores por `ai_score` descendente.

---

### Módulo: Publicación y Exportación

#### Requisito 23: Publicación en LinkedIn (RF-23)
**Historia de Usuario:** Como editor, quiero publicar en LinkedIn directamente desde el panel, para eliminar el paso manual de copiar y pegar.

**Criterios de Aceptación**
1. WHEN un EDITOR hace POST a `/api/v1/drafts/{id}/publish` para un borrador de LinkedIn APPROVED, THE system SHALL invocar LinkedIn API v2.
2. THE system SHALL usar el endpoint `POST /ugcPosts` con el `access_token` configurado.
3. IF la publicación es exitosa, THE system SHALL cambiar el estado a `PUBLISHED` y registrar `published_at` y `external_post_id`.
4. IF la publicación falla, THE system SHALL registrar el error en `publications.error_message`.

#### Requisito 24: Exportación JSON (RF-24)
**Historia de Usuario:** Como editor, quiero exportar borradores aprobados como JSON, para canales sin integración directa.

**Criterios de Aceptación**
1. WHEN un EDITOR hace GET a `/api/v1/drafts/export?format=json`, THE system SHALL generar un archivo JSON.
2. THE archivo SHALL contener todos los borradores APPROVED agrupados por canal.
3. THE system SHALL retornar el archivo con header `Content-Disposition: attachment`.
4. THE system SHALL soportar filtro por `week` para exportar una semana específica.

#### Requisito 25: Exportación CSV (RF-25)
**Historia de Usuario:** Como editor, quiero exportar borradores como CSV, para integración con herramientas externas.

**Criterios de Aceptación**
1. WHEN un EDITOR hace GET a `/api/v1/drafts/export?format=csv`, THE system SHALL generar un archivo CSV.
2. THE archivo SHALL incluir columnas: canal, título, contenido, estado, fecha.
3. THE system SHALL usar OpenCSV para la generación del archivo.

#### Requisito 26: Estado de Publicación (RF-26)
**Historia de Usuario:** Como editor, quiero ver si un borrador fue publicado exitosamente, para confirmar la distribución.

**Criterios de Aceptación**
1. THE system SHALL registrar en `publications` cada intento de publicación.
2. THE system SHALL mostrar el estado de publicación en el detalle del borrador.
3. THE system SHALL mostrar el `external_post_id` y `published_at` cuando sea exitoso.

#### Requisito 27: Reintento de Publicación (RF-27 - Backlog)
**Historia de Usuario:** Como sistema, quiero reintentar la publicación hasta 3 veces con backoff exponencial, para manejar fallos transitorios.

**Criterios de Aceptación**
1. IF la publicación falla, THE system SHALL reintentar hasta 3 veces.
2. THE system SHALL usar backoff exponencial: 1min, 2min, 4min entre reintentos.
3. THE system SHALL registrar el `retry_count` en la tabla `publications`.

---

### Módulo: Autenticación y Administración

#### Requisito 28: Autenticación JWT (RF-28)
**Historia de Usuario:** Como usuario, quiero autenticarme con email/contraseña y obtener JWT, para acceder al panel.

**Criterios de Aceptación**
1. WHEN un request POST a `/api/v1/auth/login` es recibido con credenciales válidas, THE AuthService SHALL retornar un JWT con expiración de 8 horas.
2. IF las credenciales son inválidas, THE system SHALL retornar HTTP 401.
3. THE JWT SHALL incluir claims: `sub` (userId), `role` (ADMIN/EDITOR).
4. ALL endpoints excepto `/api/v1/auth/**` SHALL requerir JWT válido.

#### Requisito 29: Roles de Usuario (RF-29)
**Historia de Usuario:** Como sistema, quiero soportar roles ADMIN y EDITOR, para control de acceso.

**Criterios de Aceptación**
1. THE system SHALL soportar roles: ADMIN (acceso total), EDITOR (revisión de borradores).
2. WHEN un EDITOR intenta acceder a endpoints de administración, THE system SHALL retornar HTTP 403.
3. THE system SHALL extraer el rol del JWT en cada request.

#### Requisito 30: Gestión de Usuarios (RF-30)
**Historia de Usuario:** Como ADMIN, quiero crear, editar, desactivar usuarios y asignar roles, para controlar acceso.

**Criterios de Aceptación**
1. WHEN un ADMIN hace GET a `/api/v1/admin/users`, THE system SHALL listar todos los usuarios.
2. WHEN un ADMIN hace POST a `/api/v1/admin/users`, THE system SHALL crear un nuevo usuario con rol asignado.
3. WHEN un ADMIN hace PATCH a `/api/v1/admin/users/{id}`, THE system SHALL actualizar el usuario.
4. THE system SHALL almacenar passwords con hash bcrypt.

#### Requisito 31: Configuración de Fuentes (RF-31)
**Historia de Usuario:** Como ADMIN, quiero agregar/editar/eliminar fuentes de datos comunitarios, para conectar plataformas.

**Criterios de Aceptación**
1. WHEN un ADMIN hace POST a `/api/v1/admin/sources`, THE system SHALL crear fuente con credenciales encriptadas (AES-256).
2. WHEN un ADMIN hace PUT a `/api/v1/admin/sources/{id}`, THE system SHALL actualizar la configuración.
3. WHEN un ADMIN hace DELETE a `/api/v1/admin/sources/{id}`, THE system SHALL desactivar la fuente y eliminar credenciales.
4. THE system SHALL validar la conexión antes de guardar las credenciales.

#### Requisito 32: Ejecución Manual del Pipeline (RF-32)
**Historia de Usuario:** Como ADMIN, quiero ejecutar el pipeline manualmente, para pruebas o contenido fuera de ciclo semanal.

**Criterios de Aceptación**
1. WHEN un ADMIN hace POST a `/api/v1/executions/trigger`, THE system SHALL ejecutar el pipeline inmediatamente.
2. THE system SHALL retornar el `executionId` para seguimiento.
3. THE system SHALL registrar quién disparó la ejecución manual en `weekly_executions.triggered_by`.

#### Requisito 33: Historial de Ejecuciones (RF-33)
**Historia de Usuario:** Como ADMIN, quiero ver el historial de ejecuciones del pipeline, para diagnosticar errores.

**Criterios de Aceptación**
1. WHEN un ADMIN hace GET a `/api/v1/executions`, THE system SHALL listar ejecuciones con filtros por estado y fecha.
2. WHEN un ADMIN hace GET a `/api/v1/executions/{id}`, THE system SHALL mostrar detalle con: estado, duración, errores.
3. THE system SHALL registrar cada ejecución en `weekly_executions` con: status, started_at, completed_at.

---

## Requisitos No Funcionales

### Rendimiento

**RNF-01**: THE 95% de las peticiones a la API SHALL responder en menos de 500ms bajo carga normal (hasta 50 usuarios concurrentes).

**RNF-02**: THE pipeline completo (recolección + análisis IA + generación) SHALL completarse en menos de 60 minutos.

**RNF-03**: THE tiempo de carga inicial del panel editorial (LCP) SHALL ser inferior a 2 segundos en conexión de 10 Mbps.

### Disponibilidad y Fiabilidad

**RNF-04**: THE sistema SHALL garantizar 99.5% de disponibilidad mensual (excluye ventanas de mantenimiento programadas).

**RNF-05**: IF el proveedor LLM no responde, THE pipeline SHALL registrar el error, notificar al ADMIN y NO perder los datos recolectados.

**RNF-06**: THE datos recolectados SHALL persistirse antes del procesamiento IA; un fallo parcial NO eliminará la actividad cruda.

### Seguridad

**RNF-07**: ALL rutas de API (excepto `/auth/login`) SHALL requerir JWT válido.

**RNF-08**: LAS API keys de LLM y LinkedIn SHALL almacenarse en variables de entorno o gestor de secretos (Vault/AWS Secrets Manager), NUNCA en texto claro.

**RNF-09**: TODA comunicación cliente-servidor SHALL ocurrir sobre HTTPS/TLS 1.2+.

**RNF-10**: TODA acción del editor (editar, aprobar, rechazar, publicar) SHALL registrarse en un log de auditoría inmutable.

### Escalabilidad y Mantenibilidad

**RNF-11**: LA aplicación SHALL desplegarse como contenedores Docker para facilitar escalado horizontal.

**RNF-12**: EL backend SHALL mantener cobertura de tests unitarios >= 80% (JUnit 5 + Mockito).

**RNF-13**: LA API REST SHALL estar documentada automáticamente con OpenAPI 3 (Springdoc).

**RNF-14**: EL sistema SHALL exponer métricas Prometheus y trazas compatibles con Grafana/Jaeger.

### Usabilidad

**RNF-15**: EL panel editorial SHALL ser usable en pantallas de 1280px o superiores (diseño responsivo).

**RNF-16**: LA interfaz y los borradores generados SHALL estar en español por defecto.

**RNF-17**: EL frontend SHALL cumplir con WCAG 2.1 nivel AA para los componentes principales.

---

## Propiedades de Corrección (Property-Based Testing)

### Pipeline: Propiedades de Ejecución

1. **Round-trip de estados**: Para toda ejecución, el estado final SHALL ser uno de: `COMPLETED` o `FAILED`. Formalmente: `execution.status ∈ {COMPLETED, FAILED}`.

2. **Invariante de persistencia**: Los datos recolectados SHALL persistirse antes del análisis IA. Formalmente: `execution.activities.size() > 0 → execution.analysis != null`.

3. **Idempotencia de ejecución**: Ejecutar el pipeline dos veces con los mismos parámetros SHALL producir dos ejecuciones independientes con diferentes `executionId`.

### Drafts: Propiedades de Borradores

4. **Invariante de estado**: Un borrador con `status=PUBLISHED` SHALL tener siempre `published_at != null` y `external_post_id != null`. Formalmente: `draft.status == PUBLISHED → draft.publishedAt != null && draft.externalPostId != null`.

5. **Metamórfica de canales**: El contenido de un borrador de Twitter SHALL tener siempre <= 280 caracteres. Formalmente: `draft.channel == TWITTER → draft.content.length() <= 280`.

6. **Monotonicidad de versiones**: El número de versión de un borrador SHALL ser siempre creciente. Formalmente: `∀ v1, v2 ∈ draft.versions: v1.versionNumber < v2.versionNumber → v1.editedAt < v2.editedAt`.

### Publicaciones: Propiedades de Publicación

7. **Invariante de publicación**: Solo borradores con `status=APPROVED` SHALL poder publicarse. Formalmente: `draft.status != APPROVED → publication == null`.

8. **Idempotencia de publicación**: Publicar un borrador ya publicado SHALL retornar el mismo `external_post_id` sin duplicar la publicación.

---

## Usuarios del Sistema

| Rol | Descripción | Acciones Principales |
|-----|-------------|----------------------|
| Editor de Contenido | Revisa y aprueba borradores generados cada semana | Ver borradores, editar texto, aprobar/rechazar, activar publicación |
| Administrador del Sistema | Configura el pipeline, canales y credenciales | Configurar fuentes, programar ejecuciones, gestionar usuarios, ver logs |
| Miembro de la Comunidad | Genera contenido que alimenta el pipeline (actor indirecto) | Generar posts, recursos, preguntas |
| Sistema LLM (Externo) | API de IA que analiza y genera texto | Recibir prompts, devolver análisis y borradores |

---

## Historias de Usuario Completas

### Editor de Contenido

| ID | Rol | Acción | Beneficio | Criterios Ac. |
|----|-----|---------|-----------|----------------|
| US-01 | Editor | Ver todos los borradores de la semana en un panel único | Revisar eficientemente sin buscar en múltiples archivos | Panel muestra lista paginada con canal, estado y fecha |
| US-02 | Editor | Leer borrador completo con contexto de origen | Evaluar si el contenido es fiel a las contribuciones | Vista detalle muestra borrador + actividad fuente |
| US-03 | Editor | Editar texto del borrador directamente en el panel | Ajustar tono o precisión sin herramientas externas | Editor inline con historial de cambios guardado |
| US-04 | Editor | Aprobar borrador con un solo clic | Agilizar flujo de publicación | Botón Aprobar cambia estado a APPROVED |
| US-05 | Editor | Rechazar borrador e indicar motivo | Para que el sistema aprenda y mejore | Modal de rechazo con campo de comentario obligatorio |
| US-06 | Editor | Publicar en LinkedIn directamente | Eliminar paso manual de copiar y pegar | Botón Publicar invoca LinkedIn API |
| US-07 | Editor | Exportar borradores aprobados como JSON/CSV | Para canales sin integración directa | Botón Exportar genera archivo descargable |
| US-08 | Editor | Recibir notificación el lunes por la mañana | Saber cuándo está disponible el contenido | Email/Slack notification con resumen |

### Administrador del Sistema

| ID | Rol | Acción | Beneficio | Criterios Ac. |
|----|-----|---------|-----------|----------------|
| US-09 | Administrador | Configurar fuentes de datos comunitarios | Conectar el sistema con la plataforma usada | Interfaz para agregar/editar fuentes con credenciales |
| US-10 | Administrador | Ajustar prompts del LLM por canal | Personalizar tono y formato | Editor de plantillas con variables dinámicas |
| US-11 | Administrador | Ver historial de ejecuciones del pipeline | Diagnosticar errores y monitorear salud | Pantalla de logs con filtros por fecha y estado |
| US-12 | Administrador | Ejecutar pipeline manualmente | Para pruebas o contenido fuera del ciclo semanal | Botón Run Now con confirmación |
| US-13 | Administrador | Gestionar usuarios editores | Controlar acceso al panel de revisión | CRUD de usuarios con asignación de roles |

---

*Documento de Requisitos - TalentCircle Content Pipeline v1.0 | Abril 2025*
