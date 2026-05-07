# Implementation Plan: Frontend–Backend Integration

## Overview

Replace all mock data in the TalentCircle React frontend with real HTTP calls to the Spring Boot backend at `http://localhost:8080`. The work is structured in six incremental phases: test infrastructure setup, HTTP client + types, Zustand store refactor, authentication, per-page data fetching, and a final wiring checkpoint. Each phase builds on the previous one so there is no orphaned code at any point.

Stack: React 19 · Vite · JavaScript (`.jsx`/`.js`) · TypeScript (type definitions only) · Zustand v5 · Axios v1.15 · React Router DOM v7 · Vitest · fast-check.

---

## Tasks

- [x] 1. Set up testing infrastructure
  - Install `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom`, and `fast-check` as dev dependencies in `frontend/talentcircle-app`
  - Add a `vitest.config.js` (or extend `vite.config.js`) with `environment: 'jsdom'` and `setupFiles` pointing to a `src/test/setup.js` that imports `@testing-library/jest-dom`
  - Add a `"test": "vitest --run"` script to `package.json`
  - Verify the test runner executes with a trivial passing test
  - _Requirements: cross-cutting (enables all test sub-tasks below)_

- [x] 2. Create TypeScript type definitions
  - Create `src/types/api.ts` with all interfaces: `LoginResponse`, `UserDto`, `DraftSummaryDto`, `DraftDetailDto`, `DraftSourceDto`, `DraftVersionDto`, `SourceDto`, `ConfigDto`, `ExecutionSummaryDto`, `CommunityActivityDto`, `ApiErrorResponse`
  - Delete `src/types/Users.ts` and `src/services/userService.ts` (superseded by `UserDto` and `adminApi.js`)
  - _Requirements: 3.1–3.12_

- [x] 3. Implement the Axios API client
  - Create `src/api/apiClient.js` with `baseURL: 'http://localhost:8080'` and `Content-Type: application/json`
  - Add request interceptor: read `accessToken` from `localStorage` and attach `Authorization: Bearer <token>` header when present
  - Add response error interceptor implementing the full classification table:
    - HTTP 401 + `refreshToken` present + not already retrying → `POST /api/v1/auth/refresh`, store new tokens, set `_retry = true`, retry original request
    - HTTP 401 + no `refreshToken`, or refresh itself returns 401 → clear `localStorage`, call `useAppStore.getState().logout()`, redirect to `/login`
    - HTTP 403 → `showToast` with "No tienes permisos para realizar esta acción."
    - HTTP 4xx/5xx → extract `error.response.data.message` or fall back to "Error de conexión. Intenta de nuevo."
    - Network error (no response) → "Sin conexión con el servidor. Verifica que el backend esté activo."
    - Re-throw all errors after toasting
  - _Requirements: 1.1–1.6, 8.1–8.5_

  - [x] 3.1 Write property test for Authorization header injection
    - **Property 5: Authorization header injection**
    - For any non-empty `accessToken` string stored in `localStorage`, every request dispatched through `apiClient` must include `Authorization: Bearer <token>`
    - Use `fc.string({ minLength: 1 })` to generate token values; mock `axios.create` adapter to capture request headers
    - **Validates: Requirements 1.2**

  - [x] 3.2 Write property test for error message propagation
    - **Property 6: Error message propagation**
    - For any API error response containing a `message` field, the interceptor must call `showToast` with that exact `message` string (not a fallback)
    - Use `fc.string({ minLength: 1 })` for the message; mock the axios response to return a 4xx with `{ message: generatedString }`
    - **Validates: Requirements 8.1, 8.4**

  - [x] 3.3 Write unit tests for apiClient error interceptor
    - Test 401 retry flow: mock refresh endpoint to return 200, verify original request is retried with new token
    - Test 401 no-refresh redirect: verify `localStorage` is cleared and `window.location` points to `/login`
    - Test 403 message: verify `showToast` is called with the Spanish permission message
    - Test network error: verify `showToast` is called with the server-down message
    - Test fallback message: verify generic fallback when response body has no `message` field
    - _Requirements: 1.3–1.5, 8.1–8.5_

- [x] 4. Implement API service modules
  - Create `src/api/authApi.js` with `login(email, password)`, `logout()`, and `refresh(refreshToken)` functions using `apiClient`
  - Create `src/api/draftsApi.js` with `list(filters)`, `getDetail(id)`, `approve(id)`, `reject(id, reason)`, and `updateContent(id, content)` functions
  - Create `src/api/adminApi.js` with `getUsers()`, `createUser(data)`, `updateUser(id, data)`, `getSources()`, `createSource(data)`, `updateSource(id, data)`, `getConfig()`, `updateConfig(data)`, `getExecutions()`, and `triggerExecution(email)` functions
  - Create `src/api/collectorApi.js` with `getActivities(executionId)` function
  - _Requirements: 2.1, 4.1, 4.5–4.8, 5.1, 5.3–5.7, 5.9, 6.1, 6.4, 7.1–7.2_

  - [x] 4.1 Write unit tests for authApi
    - Test `login` success: verify correct endpoint and payload
    - Test `login` 401: verify error propagates (toast handled by interceptor)
    - Test `logout`: verify it always calls the endpoint with the `Authorization` header
    - _Requirements: 2.1, 2.5_

  - [x] 4.2 Write unit tests for draftsApi
    - Test `list` with no filters: verify clean GET with no query params
    - Test `list` with channel + status filters: verify both params appear in the request URL
    - Test `approve`, `reject` (with reason), and `updateContent`: verify correct HTTP method, endpoint, and payload
    - _Requirements: 4.1, 4.4–4.8_

  - [x] 4.3 Write property test for filter parameter passing
    - **Property 9: Filter parameters passed to API**
    - For any valid combination of `channel` (NEWSLETTER, LINKEDIN, TWITTER, or absent) and `status` (PENDING, APPROVED, REJECTED, PUBLISHED, or absent), the resulting GET call must include exactly those values as query params and omit unset params
    - Use `fc.option(fc.constantFrom('NEWSLETTER','LINKEDIN','TWITTER'))` and `fc.option(fc.constantFrom('PENDING','APPROVED','REJECTED','PUBLISHED'))`
    - **Validates: Requirements 4.4**

- [x] 5. Refactor Zustand store
  - Remove `INITIAL_DRAFTS`, `feedItems`, and `executions` mock data constants from `useAppStore.js`
  - Add `accessToken: null` and `refreshToken: null` fields to the store's initial state
  - Replace the existing `login(user)` action with `login(loginResponse)` that stores `accessToken`, `refreshToken`, `expiresIn`, and `user` in state AND persists `accessToken` and `refreshToken` to `localStorage`
  - Update `logout()` to clear `accessToken` and `refreshToken` from both state and `localStorage`
  - Add `initAuth()` action: reads `accessToken` and `refreshToken` from `localStorage`; if both are present, sets `isAuthenticated: true` and stores them in state
  - Add `setDrafts(drafts)`, `setExecutions(executions)`, and `setFeedItems(feedItems)` setters (pages own their local state but can sync to store)
  - Keep `updateDraftStatus`, `updateDraftContent`, `showToast`, `openModal`, `closeModal` unchanged
  - _Requirements: 2.2, 2.4, 2.5, 2.7_

  - [x] 5.1 Write property test for token persistence after login
    - **Property 1: Token persistence after login**
    - For any `LoginResponse` with any `accessToken` and `refreshToken` string values, after `login(response)` is called, `localStorage.getItem('accessToken')` must equal `response.accessToken` and `localStorage.getItem('refreshToken')` must equal `response.refreshToken`
    - Use `fc.record({ accessToken: fc.string({ minLength: 1 }), refreshToken: fc.string({ minLength: 1 }), expiresIn: fc.string(), user: fc.record({...}) })`
    - **Validates: Requirements 2.2**

  - [x] 5.2 Write property test for token absence after logout
    - **Property 2: Token absence after logout**
    - For any authenticated session state (any stored token values), after `logout()` is called, `localStorage.getItem('accessToken')` must be `null`, `localStorage.getItem('refreshToken')` must be `null`, and `isAuthenticated` must be `false`
    - **Validates: Requirements 2.5**

  - [x] 5.3 Write property test for session restoration from localStorage
    - **Property 3: Session restoration from localStorage**
    - For any pair of non-empty strings `(accessToken, refreshToken)` stored in `localStorage`, calling `initAuth()` must result in `isAuthenticated === true` and the store's token fields matching the stored values. Conversely, if either token is absent, `initAuth()` must leave `isAuthenticated === false`
    - Use `fc.string({ minLength: 1 })` for both token values; test both the positive and negative cases
    - **Validates: Requirements 2.4**

  - [x] 5.4 Write property test for user object round-trip through store
    - **Property 4: User object round-trip through store**
    - For any `UserDto` object received in a `LoginResponse`, after calling `login(response)`, `useAppStore.getState().currentUser` must deeply equal `response.user`
    - Use `fc.record({ id: fc.string(), email: fc.emailAddress(), fullName: fc.string(), role: fc.constantFrom('ADMIN','EDITOR'), active: fc.boolean() })`
    - **Validates: Requirements 2.7**

- [x] 6. Checkpoint — store and API layer complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Wire auth initialisation into App.jsx and update Login page
  - In `App.jsx`, add a `useEffect` that calls `useAppStore.getState().initAuth()` on mount so the protected route check reads real token state before the first render
  - In `Login.jsx`, replace the mock `setTimeout` with a real `authApi.login(email, password)` call; on success call the `login(response)` store action and `navigate('/dashboard')`; on error let the interceptor handle the toast; in `finally` set `loading = false`
  - Ensure the submit button is disabled and shows a spinner while the request is in flight
  - _Requirements: 2.1–2.3, 2.6_

  - [x] 7.1 Write component tests for Login page
    - Test: loading spinner appears and submit button is disabled while request is in-flight
    - Test: `navigate('/dashboard')` is called on successful login response
    - Test: button re-enables after error (toast handled by interceptor, not Login)
    - _Requirements: 2.1, 2.3, 2.6_

- [x] 8. Integrate Drafts page with real API
  - Replace the Zustand `drafts` store read with local `useState` + `useEffect` that calls `draftsApi.list()` on mount
  - Implement the `cancelled` flag pattern and `finally` block to guarantee `loading` is always set to `false`
  - Show a loading skeleton while `loading === true`
  - Wire channel and status filter controls to re-call `draftsApi.list({ channel, status })` when filter values change
  - Update all field references from mock shape (`preview`, `score`, `channelLabel`) to API shape (`summary`, `aiScore`, `channel`); derive channel labels from the `channel` enum value in the component
  - Wire "Aprobar" button: call `draftsApi.approve(id)`, then `updateDraftStatus(id, 'APPROVED')` on success; use `actionLoading` boolean to disable button while in-flight
  - Wire "Rechazar" button: call `draftsApi.reject(id, 'Rechazado desde el panel')`, then `updateDraftStatus(id, 'REJECTED')` on success; use `actionLoading` boolean
  - When DraftModal opens, call `draftsApi.getDetail(id)` to load full detail (sources, version history)
  - When DraftModal saves, call `draftsApi.updateContent(id, content)` before closing; call `updateDraftContent(id, content)` on success
  - Show success toast after each successful draft action
  - _Requirements: 4.1–4.10, 9.1–9.4_

  - [x] 8.1 Write property test for loading flag always false after fetch
    - **Property 7: Loading flag always false after fetch**
    - For any page-level data fetch (success or error), after the fetch settles, the local `loading` state must be `false`
    - Mock `draftsApi.list` to randomly resolve or reject; render `Drafts` with React Testing Library; assert `loading` is `false` after `waitFor`
    - **Validates: Requirements 9.2**

  - [x] 8.2 Write property test for independent loading states
    - **Property 8: Independent loading states**
    - Render `Drafts` and `Admin` simultaneously; trigger a fetch on one; assert the other's `loading` state is unaffected
    - **Validates: Requirements 9.4**

  - [x] 8.3 Write component tests for Drafts page
    - Test: skeleton renders while `loading === true`
    - Test: empty state renders when API returns `[]`
    - Test: filter chip change triggers `draftsApi.list` with correct params
    - Test: "Aprobar" button is disabled while `actionLoading === true`
    - _Requirements: 4.2–4.6, 9.1, 9.3_

- [x] 9. Integrate Executions page with real API
  - Replace the Zustand `executions` store read with local `useState` + `useEffect` that calls `adminApi.getExecutions()` on mount
  - Implement the `cancelled` flag pattern and `finally` block
  - Show a loading skeleton while `loading === true`
  - Wire "Disparar ejecución manual" button: call `adminApi.triggerExecution(currentUser.email)` on click; show success toast on HTTP 202; the interceptor's standard message extraction handles the HTTP 400 "Ya hay una ejecución en curso" message automatically
  - Disable the trigger button and show a spinner while the request is in-flight (`actionLoading`)
  - _Requirements: 6.1–6.5, 9.1–9.3_

  - [x] 9.1 Write component tests for Executions page
    - Test: skeleton renders while `loading === true`
    - Test: trigger button is disabled while `actionLoading === true`
    - Test: success toast is shown on HTTP 202 response
    - _Requirements: 6.2, 6.4, 9.1, 9.3_

- [x] 10. Integrate Admin page with real API
  - On mount, fire `adminApi.getUsers()`, `adminApi.getSources()`, and `adminApi.getConfig()` in parallel using `Promise.all`; each section has its own `loading` state
  - Show loading skeletons for each section independently while its fetch is in flight
  - Wire source active toggle: call `adminApi.updateSource(id, { active: newValue })` and update local sources state on success
  - Wire "Guardar cambios" (pipeline config): call `adminApi.updateConfig(fullConfigPayload)` and show success toast on HTTP 200
  - Wire "Guardar" (prompt tab): call `adminApi.updateConfig({ ...currentConfig, [promptField]: newValue })` and show success toast on HTTP 200
  - Wire "Nuevo usuario": call `adminApi.createUser(data)` and refresh users list on success
  - Wire "Editar" user row: call `adminApi.updateUser(id, data)` and refresh users list on success
  - Wire "Agregar fuente": call `adminApi.createSource(data)` and refresh sources list on success
  - _Requirements: 5.1–5.9, 9.1–9.4_

  - [x] 10.1 Write component tests for Admin page
    - Test: three parallel fetches fire on mount (spy on `adminApi.getUsers`, `getSources`, `getConfig`)
    - Test: source toggle calls `adminApi.updateSource` with correct `{ active }` payload
    - Test: config save calls `adminApi.updateConfig` with the full config payload
    - _Requirements: 5.1–5.5, 9.1_

- [x] 11. Integrate Dashboard page with real API
  - On mount, call `draftsApi.list()` and `adminApi.getExecutions()` in parallel
  - After executions resolve, identify the most recent execution (greatest `startedAt` value) and call `collectorApi.getActivities(mostRecentExecution.id)`
  - Show loading skeletons for the stats grid and feed section while fetches are in flight
  - Derive "Borradores Generados" from `drafts.length`
  - Derive "Pendientes de Revisión" from `drafts.filter(d => d.status === 'PENDING').length`
  - Display the three most recent drafts in "Borradores de la Semana" (sort by `createdAt` descending, take first 3)
  - Map `CommunityActivityDto` fields to the feed item display (use `reactionCount`, `responseCount`, `shareCount`, `author`, `title`, `content`)
  - _Requirements: 7.1–7.7, 9.1–9.4_

  - [x] 11.1 Write property test for pending count derivation
    - **Property 10: Dashboard stat derivation — pending count**
    - For any array of `DraftSummaryDto` objects, the "Pendientes de Revisión" count must equal the number of items where `status === 'PENDING'`
    - Extract the derivation logic into a pure helper function `derivePendingCount(drafts)` and test it with `fc.array(fc.record({ status: fc.constantFrom('PENDING','APPROVED','REJECTED','PUBLISHED'), ... }))`
    - **Validates: Requirements 7.6**

  - [x] 11.2 Write property test for most-recent execution selection
    - **Property 11: Dashboard most-recent execution used for activities**
    - For any non-empty array of `ExecutionSummaryDto` objects, the `getActivities` call must use the `id` of the execution with the lexicographically greatest `startedAt` value
    - Extract the selection logic into a pure helper function `getMostRecentExecution(executions)` and test it with `fc.array(fc.record({ id: fc.string(), startedAt: fc.date().map(d => d.toISOString()), ... }), { minLength: 1 })`
    - **Validates: Requirements 7.2**

  - [x] 11.3 Write component tests for Dashboard page
    - Test: stat cards display correct derived values from mock API data
    - Test: `collectorApi.getActivities` is called with the most recent execution's id
    - Test: skeletons render while fetches are in flight
    - _Requirements: 7.1–7.7, 9.1_

- [x] 12. Final checkpoint — full integration complete
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at the API layer and after full page integration
- Property tests validate universal correctness properties using fast-check (minimum 100 iterations per property)
- Unit/component tests validate specific examples, edge cases, and error conditions
- The `cancelled` flag pattern in every `useEffect` prevents state updates on unmounted components
- Field name mapping: mock shape used `preview`/`score`/`channelLabel` — API shape uses `summary`/`aiScore`/`channel` (labels derived in component)
