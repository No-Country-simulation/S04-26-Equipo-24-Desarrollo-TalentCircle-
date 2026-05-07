// Feature: frontend-backend-integration, Property 5: Authorization header injection
// Validates: Requirements 1.2

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import * as fc from 'fast-check'

// Mock the store before importing apiClient so the module-level import resolves
vi.mock('../../store/useAppStore', () => ({
  useAppStore: {
    getState: () => ({
      showToast: vi.fn(),
      logout: vi.fn(),
    }),
  },
}))

// Import apiClient AFTER mocking its dependency
import apiClient from '../apiClient'

describe('apiClient — Property 5: Authorization header injection', () => {
  // Capture the headers sent by each request via a custom adapter
  let capturedHeaders = null

  beforeEach(() => {
    capturedHeaders = null
    localStorage.clear()

    // Replace the axios instance adapter with a spy that records request headers
    // and immediately resolves with a 200 response (no real HTTP call).
    apiClient.defaults.adapter = (config) => {
      capturedHeaders = { ...config.headers }
      return Promise.resolve({
        data: {},
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      })
    }
  })

  afterEach(() => {
    localStorage.clear()
    // Restore the default adapter so other tests are not affected
    delete apiClient.defaults.adapter
  })

  it(
    'attaches Authorization: Bearer <token> for every non-empty accessToken',
    async () => {
      await fc.assert(
        fc.asyncProperty(
          // Constrain to tokens that Axios will not modify when setting headers:
          // - at least one non-whitespace character (so the token is truthy and meaningful)
          // - no leading/trailing whitespace (Axios trims header values)
          // Real JWT access tokens always satisfy these constraints.
          fc.string({ minLength: 1 }).filter((s) => s === s.trim() && s.trim().length > 0),
          async (token) => {
            // Arrange: store the generated token in localStorage
            localStorage.setItem('accessToken', token)

            // Act: dispatch any request through apiClient
            await apiClient.get('/test-endpoint')

            // Assert: the Authorization header must be exactly "Bearer <token>"
            expect(capturedHeaders['Authorization']).toBe(`Bearer ${token}`)
          }
        ),
        { numRuns: 100 }
      )
    }
  )

  it(
    'does NOT attach an Authorization header when no accessToken is in localStorage',
    async () => {
      // Ensure localStorage has no token
      localStorage.removeItem('accessToken')

      await apiClient.get('/test-endpoint')

      expect(capturedHeaders['Authorization']).toBeUndefined()
    }
  )
})
