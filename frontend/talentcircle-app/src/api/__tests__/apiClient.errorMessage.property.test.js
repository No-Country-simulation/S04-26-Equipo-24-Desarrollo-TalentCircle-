// Feature: frontend-backend-integration, Property 6: Error message propagation
// Validates: Requirements 8.1, 8.4

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import * as fc from 'fast-check'

// Capture the showToast spy so we can inspect calls from the interceptor
let showToastSpy = vi.fn()

// Mock the store before importing apiClient so the module-level import resolves
vi.mock('../../store/useAppStore', () => ({
  useAppStore: {
    getState: () => ({
      showToast: showToastSpy,
      logout: vi.fn(),
    }),
  },
}))

// Import apiClient AFTER mocking its dependency
import apiClient from '../apiClient'

describe('apiClient — Property 6: Error message propagation', () => {
  beforeEach(() => {
    showToastSpy = vi.fn()
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
    // Restore the default adapter so other tests are not affected
    delete apiClient.defaults.adapter
  })

  it(
    'calls showToast with the exact message from the API error response body (not the fallback)',
    async () => {
      await fc.assert(
        fc.asyncProperty(
          // Generate any non-empty string as the error message
          fc.string({ minLength: 1 }),
          // Generate a 4xx status code (400–499) to trigger the general error handler
          fc.integer({ min: 400, max: 499 }).filter((s) => s !== 401 && s !== 403),
          async (errorMessage, statusCode) => {
            // Reset the spy for each iteration
            showToastSpy.mockClear()

            // Arrange: install an adapter that rejects with a 4xx error
            // shaped exactly as Axios shapes real HTTP errors:
            //   error.response.data.message = errorMessage
            apiClient.defaults.adapter = (_config) => {
              const error = new Error('Request failed')
              error.response = {
                status: statusCode,
                data: { message: errorMessage },
                headers: {},
              }
              error.config = _config
              return Promise.reject(error)
            }

            // Act: dispatch any request — the interceptor will handle the error
            await apiClient.get('/test-endpoint').catch(() => {
              // Expected rejection — we only care about the showToast side-effect
            })

            // Assert: showToast must have been called with the exact message string,
            // NOT the fallback "Error de conexión. Intenta de nuevo."
            expect(showToastSpy).toHaveBeenCalledOnce()
            const [, , calledMessage] = showToastSpy.mock.calls[0]
            expect(calledMessage).toBe(errorMessage)
            expect(calledMessage).not.toBe('Error de conexión. Intenta de nuevo.')
          }
        ),
        { numRuns: 100 }
      )
    }
  )

  it(
    'uses the fallback message when the API error response body has no message field',
    async () => {
      // Install an adapter that rejects with a 4xx error but no message in body
      apiClient.defaults.adapter = (_config) => {
        const error = new Error('Request failed')
        error.response = {
          status: 400,
          data: {},
          headers: {},
        }
        error.config = _config
        return Promise.reject(error)
      }

      await apiClient.get('/test-endpoint').catch(() => {})

      expect(showToastSpy).toHaveBeenCalledOnce()
      const [, , calledMessage] = showToastSpy.mock.calls[0]
      expect(calledMessage).toBe('Error de conexión. Intenta de nuevo.')
    }
  )
})
