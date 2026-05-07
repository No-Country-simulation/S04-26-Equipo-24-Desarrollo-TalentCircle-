# Requirements Document

## Introduction

The TalentCircle frontend is a React 19 + Vite + JavaScript application that currently runs entirely on hardcoded mock data stored in a Zustand store. The backend is a fully implemented Spring Boot 3.3 + Java 21 API running at `http://localhost:8080`. This feature replaces every mock data source with real HTTP calls to the backend, wires up JWT-based authentication, and adds proper loading states, error handling, and TypeScript type definitions for all API responses.

The scope covers: HTTP client infrastructure, authentication flow, drafts management, admin panel (users, sources, pipeline config, executions), dashboard stats and activity feed, and cross-cutting concerns (error display, loading states, TypeScript types).

---

## Glossary

- **API_Client**: The configured Axios instance used by all service modules to communicate with the backend.
- **Auth_Service**: The frontend service module responsible for login, logout, and token refresh calls.
- **Auth_Store**: The Zustand slice that holds `isAuthenticated`, `currentUser`, `accessToken`, and `refreshToken`.
- **Draft_Service**: The frontend service module responsible for all `/api/v1/drafts` calls.
- **Admin_Service**: The frontend service module responsible for all `/api/v1/admin` calls.
- **Dashboard_Service**: The frontend service module responsible for fetching stats and activity feed data.
- **Toast_System**: The existing `showToast` mechanism in `useAppStore` used to surface error and success messages.
- **Loading_State**: A boolean flag per data-fetching operation that controls skeleton/spinner rendering.
- **JWT**: JSON Web Token — the `accessToken` returned by the backend on login, valid for 8 hours.
- **Refresh_Token**: A UUID token returned alongside the JWT, valid for 7 days, used to obtain a new JWT without re-login.
- **localStorage**: The browser storage used to persist `accessToken` and `refreshToken` across page refreshes.
- **Protected_Route**: A React Router route that redirects unauthenticated users to `/login`.
- **Draft**: A generated content piece for one of three channels: `NEWSLETTER`, `LINKEDIN`, or `TWITTER`.
- **Execution**: A pipeline run record returned by `GET /api/v1/admin/executions`.
- **Community_Activity**: A collected community post, question, or resource returned by `GET /api/v1/admin/collector/activities`.

---

## Requirements

### Requirement 1: HTTP Client Infrastructure

**User Story:** As a frontend developer, I want a single configured Axios instance with JWT injection and 401 handling, so that all API calls share consistent authentication and error recovery behavior.

#### Acceptance Criteria

1. THE API_Client SHALL set `http://localhost:8080` as the base URL for all requests.
2. WHEN a request is dispatched, THE API_Client SHALL attach the `Authorization: Bearer <accessToken>` header if an `accessToken` exists in localStorage.
3. WHEN a response returns HTTP 401 and a `refreshToken` exists in localStorage, THE API_Client SHALL call `POST /api/v1/auth/refresh` once, store the new tokens, and retry the original request with the new `accessToken`.
4. WHEN a response returns HTTP 401 and no valid `refreshToken` exists, THE API_Client SHALL clear localStorage, update the Auth_Store to unauthenticated, and redirect the user to `/login`.
5. IF the token refresh call itself returns HTTP 401, THEN THE API_Client SHALL clear localStorage, update the Auth_Store to unauthenticated, and redirect the user to `/login`.
6. THE API_Client SHALL set `Content-Type: application/json` as the default request header.

---

### Requirement 2: Authentication Integration

**User Story:** As a user, I want to log in with my real credentials and have my session persist across page refreshes, so that I do not have to re-authenticate every time I open the application.

#### Acceptance Criteria

1. WHEN the user submits the login form, THE Auth_Service SHALL call `POST /api/v1/auth/login` with `{ email, password }`.
2. WHEN the login response returns HTTP 200, THE Auth_Store SHALL store `accessToken`, `refreshToken`, `expiresIn`, and `user` from the response, persist `accessToken` and `refreshToken` to localStorage, and navigate to `/dashboard`.
3. IF the login response returns HTTP 401, THEN THE Auth_Store SHALL display an error message via the Toast_System with the text "Credenciales inválidas".
4. WHEN the application initialises, THE Auth_Store SHALL read `accessToken` and `refreshToken` from localStorage and restore the authenticated session if both values are present.
5. WHEN the user triggers logout, THE Auth_Service SHALL call `POST /api/v1/auth/logout` with the `Authorization` header, then clear localStorage and reset the Auth_Store to unauthenticated regardless of the response status.
6. WHILE the login request is in flight, THE Login page SHALL display a loading spinner and disable the submit button.
7. THE Auth_Store SHALL expose the authenticated `user` object (id, email, fullName, role) to all components that consume it.

---

### Requirement 3: TypeScript Type Definitions

**User Story:** As a frontend developer, I want TypeScript interfaces for every API response shape, so that I get compile-time safety and IDE autocompletion when working with backend data.

#### Acceptance Criteria

1. THE Frontend SHALL define a `LoginResponse` type with fields: `accessToken: string`, `refreshToken: string`, `expiresIn: string`, `user: UserDto`.
2. THE Frontend SHALL define a `UserDto` type with fields: `id: string`, `email: string`, `fullName: string`, `role: 'ADMIN' | 'EDITOR'`, `active: boolean`.
3. THE Frontend SHALL define a `DraftSummaryDto` type with fields: `id: string`, `channel: 'NEWSLETTER' | 'LINKEDIN' | 'TWITTER'`, `status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'PUBLISHED'`, `createdAt: string`, `summary: string`.
4. THE Frontend SHALL define a `DraftDetailDto` type with fields: `id`, `channel`, `content`, `editedContent`, `status`, `aiScore`, `createdAt`, `updatedAt`, `sources: DraftSourceDto[]`, `versions: DraftVersionDto[]`.
5. THE Frontend SHALL define a `DraftSourceDto` type with fields: `id: string`, `title: string`, `relevanceScore: number`.
6. THE Frontend SHALL define a `DraftVersionDto` type with fields: `id: string`, `content: string`, `editedBy: string | null`, `editedAt: string`, `versionNumber: number`.
7. THE Frontend SHALL define a `SourceDto` type with fields: `id: string`, `name: string`, `type: 'DISCORD' | 'CIRCLE' | 'SLACK'`, `active: boolean`.
8. THE Frontend SHALL define a `ConfigDto` type with fields: `llmProvider: string`, `llmModel: string`, `newsletterPrompt: string`, `linkedinPrompt: string`, `twitterPrompt: string`, `maxItemsPerChannel: number`, `scheduleCron: string`.
9. THE Frontend SHALL define an `ExecutionSummaryDto` type with fields: `id: string`, `weekStart: string`, `weekEnd: string`, `status: 'COMPLETED' | 'RUNNING' | 'FAILED'`, `startedAt: string`, `completedAt: string | null`.
10. THE Frontend SHALL define a `CommunityActivityDto` type with fields: `id: string`, `title: string`, `content: string`, `type: 'POST' | 'QUESTION' | 'RESOURCE'`, `reactionCount: number`, `responseCount: number`, `shareCount: number`, `author: string`, `sourceUrl: string`.
11. THE Frontend SHALL define an `ApiErrorResponse` type with fields: `error: string`, `message: string`, `timestamp: string`.
12. WHERE the existing `Users.ts` type conflicts with `UserDto`, THE Frontend SHALL replace it with the canonical `UserDto` definition to maintain consistent terminology.

---

### Requirement 4: Drafts Page Integration

**User Story:** As an editor, I want the Drafts page to show real drafts from the backend and let me approve, reject, and edit them via real API calls, so that my actions are persisted and visible to other team members.

#### Acceptance Criteria

1. WHEN the Drafts page mounts, THE Draft_Service SHALL call `GET /api/v1/drafts` and populate the Zustand drafts list with the response.
2. WHILE the drafts fetch is in flight, THE Drafts page SHALL display a loading skeleton in place of the draft cards.
3. IF the drafts fetch returns an error, THEN THE Toast_System SHALL display the error message from the API response.
4. WHEN the user applies a channel or status filter, THE Draft_Service SHALL call `GET /api/v1/drafts` with the corresponding `channel` and `status` query parameters.
5. WHEN the user clicks "Aprobar" on a draft card, THE Draft_Service SHALL call `POST /api/v1/drafts/{id}/approve` and update the draft's status in the Zustand store on success.
6. WHEN the user clicks "Rechazar" on a draft card, THE Draft_Service SHALL call `POST /api/v1/drafts/{id}/reject` with `{ reason: "Rechazado desde el panel" }` and update the draft's status in the Zustand store on success.
7. WHEN the user opens the DraftModal and saves edited content, THE Draft_Service SHALL call `PATCH /api/v1/drafts/{id}/content` with the updated text before closing the modal.
8. WHEN the DraftModal opens, THE Draft_Service SHALL call `GET /api/v1/drafts/{id}` to load the full detail including sources and version history.
9. IF any draft action (approve, reject, edit) returns an error, THEN THE Toast_System SHALL display the error message from the API response.
10. WHEN a draft action succeeds, THE Toast_System SHALL display a success confirmation message.

---

### Requirement 5: Admin Panel Integration

**User Story:** As an admin, I want the Admin panel to load real users, sources, and pipeline configuration from the backend and persist any changes I make, so that the system reflects the actual state of the application.

#### Acceptance Criteria

1. WHEN the Admin page mounts, THE Admin_Service SHALL call `GET /api/v1/admin/users`, `GET /api/v1/admin/sources`, and `GET /api/v1/admin/config` in parallel and populate the respective UI sections.
2. WHILE any of the three admin data fetches are in flight, THE Admin page SHALL display loading skeletons for the corresponding sections.
3. WHEN the admin toggles a source's active state, THE Admin_Service SHALL call `PUT /api/v1/admin/sources/{id}` with `{ active: <newValue> }` and update the source list on success.
4. WHEN the admin clicks "Guardar cambios" in the pipeline config section, THE Admin_Service SHALL call `PUT /api/v1/admin/config` with the full config payload and display a success toast on HTTP 200.
5. WHEN the admin clicks "Guardar" on a prompt tab, THE Admin_Service SHALL call `PUT /api/v1/admin/config` with the updated prompt field and display a success toast on HTTP 200.
6. WHEN the admin clicks "Nuevo usuario", THE Admin_Service SHALL call `POST /api/v1/admin/users` with the new user data and refresh the users list on success.
7. WHEN the admin clicks "Editar" on a user row, THE Admin_Service SHALL call `PUT /api/v1/admin/users/{id}` with the updated fields and refresh the users list on success.
8. IF any admin API call returns an error, THEN THE Toast_System SHALL display the error message from the API response.
9. WHEN the admin clicks "Agregar fuente", THE Admin_Service SHALL call `POST /api/v1/admin/sources` with the new source data and refresh the sources list on success.

---

### Requirement 6: Executions Page Integration

**User Story:** As an admin, I want the Executions page to show the real pipeline execution history from the backend, so that I can monitor pipeline runs and trigger manual executions.

#### Acceptance Criteria

1. WHEN the Executions page mounts, THE Admin_Service SHALL call `GET /api/v1/admin/executions` and populate the executions table.
2. WHILE the executions fetch is in flight, THE Executions page SHALL display a loading skeleton in place of the table rows.
3. IF the executions fetch returns an error, THEN THE Toast_System SHALL display the error message from the API response.
4. WHEN the admin clicks "Disparar ejecución manual", THE Admin_Service SHALL call `POST /api/v1/admin/executions/trigger` with the current user's email as `triggeredBy` and display a success toast on HTTP 202.
5. IF the trigger call returns HTTP 400 (pipeline already running), THEN THE Toast_System SHALL display "Ya hay una ejecución en curso".

---

### Requirement 7: Dashboard Integration

**User Story:** As a user, I want the Dashboard to show real stats and the latest community activity feed from the backend, so that the numbers and feed items reflect actual pipeline data.

#### Acceptance Criteria

1. WHEN the Dashboard page mounts, THE Dashboard_Service SHALL call `GET /api/v1/drafts` and `GET /api/v1/admin/executions` in parallel to derive the stats displayed in the stat cards.
2. WHEN the Dashboard page mounts, THE Dashboard_Service SHALL call `GET /api/v1/admin/collector/activities` with the most recent execution's id to populate the community activity feed.
3. WHILE the dashboard data fetches are in flight, THE Dashboard page SHALL display loading skeletons for the stats grid and the feed section.
4. IF any dashboard fetch returns an error, THEN THE Toast_System SHALL display the error message from the API response.
5. THE Dashboard page SHALL derive "Borradores Generados" from the count of drafts returned by the API for the current week.
6. THE Dashboard page SHALL derive "Pendientes de Revisión" from the count of drafts with status `PENDING`.
7. THE Dashboard page SHALL display the three most recent drafts in the "Borradores de la Semana" section using data from the API.

---

### Requirement 8: Error Handling

**User Story:** As a user, I want API errors to be surfaced clearly via the existing Toast system, so that I always know when something went wrong and what the problem is.

#### Acceptance Criteria

1. WHEN any API call returns an HTTP 4xx or 5xx response, THE Toast_System SHALL display the `message` field from the `ApiErrorResponse` body if present.
2. IF the API response body does not contain a `message` field, THEN THE Toast_System SHALL display a generic fallback message: "Error de conexión. Intenta de nuevo.".
3. WHEN a network error occurs (no response received), THE Toast_System SHALL display "Sin conexión con el servidor. Verifica que el backend esté activo.".
4. THE API_Client SHALL extract error messages from the Axios error response and pass them to the Toast_System in a centralised error interceptor, so that individual service modules do not need to duplicate error-handling logic.
5. WHEN an HTTP 403 response is received, THE Toast_System SHALL display "No tienes permisos para realizar esta acción.".

---

### Requirement 9: Loading States

**User Story:** As a user, I want to see loading indicators while data is being fetched, so that I know the application is working and not frozen.

#### Acceptance Criteria

1. WHEN a page-level data fetch is initiated, THE page component SHALL set a `loading` flag to `true` and render a skeleton or spinner in place of the data.
2. WHEN the data fetch completes (success or error), THE page component SHALL set the `loading` flag to `false` and render the actual content or an empty state.
3. WHEN an action button (approve, reject, save, trigger) is clicked and the corresponding API call is in flight, THE button SHALL be disabled and display a spinner to prevent duplicate submissions.
4. THE Loading_State for each page SHALL be independent, so that loading on the Drafts page does not affect the Admin page.
