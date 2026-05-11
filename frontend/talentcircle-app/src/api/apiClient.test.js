// Unit tests for apiClient error interceptor
// Validates: Requirements 1.3, 1.4, 1.5, 8.1, 8.2, 8.3, 8.4, 8.5

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

// ─── Store mock ───────────────────────────────────────────────────────────────
// Must be declared before importing apiClient so the module-level import resolves.
let showToastSpy = vi.fn()
let logoutSpy = vi.fn()

vi.mock('../store/useAppStore', () => ({
  useAppStore: {
    getState: () => ({
      showToast: showToastSpy,
      logout: logoutSpy,
    }),
  },
}))

// Import apiClient AFTER mocking its dependency
import apiClient, { refreshClient } from './apiClient'

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Install a custom Axios adapter on the apiClient instance.
 */
function setAdapter(handler) {
  apiClient.defaults.adapter = handler
}

/**
 * Install a custom Axios adapter on the refreshClient instance.
 */
function setRefreshAdapter(handler) {
  refreshClient.defaults.adapter = handler
}

/** Build a resolved Axios response object. */
function makeResponse(status, data, config) {
  return { data, status, statusText: String(status), headers: {}, config }
}

/** Build a rejected Axios error object with a response (HTTP error). */
function makeHttpError(status, data, config) {
  const error = new Error(`Request failed with status code ${status}`)
  error.response = { status, data, headers: {}, config }
  error.config = config
  return error
}

/** Build a rejected Axios error object without a response (network error). */
function makeNetworkError(config) {
  const error = new Error('Network Error')
  // No error.response — simulates a network-level failure
  error.config = config
  return error
}

// ─── Test suite ───────────────────────────────────────────────────────────────

describe('apiClient — error interceptor unit tests', () => {
  beforeEach(() => {
    // Reset spies and localStorage before every test
    showToastSpy = vi.fn()
    logoutSpy = vi.fn()
    localStorage.clear()

    // Reset window.location.href to a neutral value
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { href: 'http://localhost/' },
    })
  })

  afterEach(() => {
    localStorage.clear()
    // Remove the custom adapter so tests don't bleed into each other
    delete apiClient.defaults.adapter
    delete refreshClient.defaults.adapter
  })

  // ── Test 1: 401 retry flow ─────────────────────────────────────────────────
  // Validates: Requirement 1.3
  describe('401 with valid refreshToken — refresh + retry', () => {
    it('retries the original request with the new accessToken after a successful refresh', async () => {
      // Arrange
      localStorage.setItem('refreshToken', 'old-refresh-token')
      localStorage.setItem('accessToken', 'old-access-token')

      const newAccessToken = 'new-access-token-xyz'
      const newRefreshToken = 'new-refresh-token-xyz'

      let callCount = 0
      let retriedRequestHeaders = null

      // apiClient adapter: first call → 401, third call (retry) → 200
      setAdapter((config) => {
        callCount++

        if (callCount === 1) {
          // Original request → 401
          return Promise.reject(makeHttpError(401, {}, config))
        }

        // Retried original request → 200
        retriedRequestHeaders = { ...config.headers }
        return Promise.resolve(makeResponse(200, { ok: true }, config))
      })

      // refreshClient adapter: refresh call → 200 with new tokens
      setRefreshAdapter((config) =>
        Promise.resolve(
          makeResponse(200, { accessToken: newAccessToken, refreshToken: newRefreshToken }, config)
        )
      )

      // Act
      const response = await apiClient.get('/some-protected-endpoint')

      // Assert: the retried request succeeded
      expect(response.data).toEqual({ ok: true })

      // Assert: new tokens were persisted to localStorage
      expect(localStorage.getItem('accessToken')).toBe(newAccessToken)
      expect(localStorage.getItem('refreshToken')).toBe(newRefreshToken)

      // Assert: the retried request used the new Authorization header
      expect(retriedRequestHeaders['Authorization']).toBe(`Bearer ${newAccessToken}`)
    })
  })

  // ── Test 2: 401 no-refresh redirect ───────────────────────────────────────
  // Validates: Requirement 1.4
  describe('401 with no refreshToken — clear localStorage + redirect /login', () => {
    it('clears localStorage and redirects to /login when no refreshToken is present', async () => {
      // Arrange: no refreshToken in localStorage
      localStorage.setItem('accessToken', 'some-access-token')
      // refreshToken intentionally NOT set

      setAdapter((config) => Promise.reject(makeHttpError(401, {}, config)))

      // Act
      await apiClient.get('/protected').catch(() => {})

      // Assert: localStorage was cleared
      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()

      // Assert: logout was called on the store
      expect(logoutSpy).toHaveBeenCalledOnce()

      // Assert: window.location was redirected to /login
      expect(window.location.href).toBe('/login')
    })

    it('clears localStorage and redirects to /login when refreshToken is an empty string', async () => {
      // Arrange: empty refreshToken (falsy)
      localStorage.setItem('accessToken', 'some-access-token')
      localStorage.setItem('refreshToken', '')

      setAdapter((config) => Promise.reject(makeHttpError(401, {}, config)))

      // Act
      await apiClient.get('/protected').catch(() => {})

      // Assert
      expect(localStorage.getItem('accessToken')).toBeNull()
      expect(logoutSpy).toHaveBeenCalledOnce()
      expect(window.location.href).toBe('/login')
    })
  })

  // ── Test 3: 403 Spanish permission message ────────────────────────────────
  // Validates: Requirement 8.5
  describe('403 response — Spanish permission toast', () => {
    it('calls showToast with the exact Spanish permission message on HTTP 403', async () => {
      // Arrange
      setAdapter((config) => Promise.reject(makeHttpError(403, {}, config)))

      // Act
      await apiClient.get('/admin-only').catch(() => {})

      // Assert
      expect(showToastSpy).toHaveBeenCalledOnce()
      const [, , message] = showToastSpy.mock.calls[0]
      expect(message).toBe('No tienes permisos para realizar esta acción.')
    })
  })

  // ── Test 4: Network error ─────────────────────────────────────────────────
  // Validates: Requirement 8.3
  describe('network error (no response) — server-down toast', () => {
    it('calls showToast with the server-down message when no response is received', async () => {
      // Arrange: adapter throws a network error (no .response property)
      setAdapter((config) => Promise.reject(makeNetworkError(config)))

      // Act
      await apiClient.get('/any-endpoint').catch(() => {})

      // Assert
      expect(showToastSpy).toHaveBeenCalledOnce()
      const [, , message] = showToastSpy.mock.calls[0]
      expect(message).toBe('Sin conexión con el servidor. Verifica que el backend esté activo.')
    })
  })

  // ── Test 5: Fallback message (no message field in body) ───────────────────
  // Validates: Requirement 8.2
  describe('5xx response with no message field — generic fallback toast', () => {
    it('calls showToast with the generic fallback when response body has no message field', async () => {
      // Arrange: 500 response body has no "message" field
      setAdapter((config) =>
        Promise.reject(makeHttpError(500, { error: 'Internal Server Error' }, config))
      )

      // Act
      await apiClient.get('/any-endpoint').catch(() => {})

      // Assert
      expect(showToastSpy).toHaveBeenCalledOnce()
      const [, , message] = showToastSpy.mock.calls[0]
      expect(message).toBe('Error de conexión. Intenta de nuevo.')
    })

    it('calls showToast with the generic fallback when response body is empty', async () => {
      // Arrange: 500 response with empty body
      setAdapter((config) => Promise.reject(makeHttpError(500, {}, config)))

      // Act
      await apiClient.get('/any-endpoint').catch(() => {})

      // Assert
      expect(showToastSpy).toHaveBeenCalledOnce()
      const [, , message] = showToastSpy.mock.calls[0]
      expect(message).toBe('Error de conexión. Intenta de nuevo.')
    })
  })
})
