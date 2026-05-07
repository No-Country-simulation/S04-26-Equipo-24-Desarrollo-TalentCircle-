/**
 * Unit tests for src/api/authApi.js
 * Requirements: 2.1, 2.5
 *
 * apiClient is mocked so no real HTTP calls are made.
 * Error toasting is handled by the apiClient interceptor — not tested here.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the apiClient module before importing authApi
vi.mock('../apiClient', () => ({
  default: {
    post: vi.fn(),
  },
}))

import apiClient from '../apiClient'
import { login, logout } from '../authApi'

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

// ─── login ────────────────────────────────────────────────────────────────────

describe('authApi.login', () => {
  it('calls POST /api/v1/auth/login with the correct endpoint and payload', async () => {
    const mockResponse = {
      data: {
        accessToken: 'tok',
        refreshToken: 'ref',
        expiresIn: '8h',
        user: { id: '1', email: 'a@b.com', fullName: 'Test', role: 'ADMIN', active: true },
      },
    }
    apiClient.post.mockResolvedValueOnce(mockResponse)

    await login('a@b.com', 'password123')

    expect(apiClient.post).toHaveBeenCalledOnce()
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/login', {
      email: 'a@b.com',
      password: 'password123',
    })
  })

  it('returns the response data on success', async () => {
    const loginData = {
      accessToken: 'tok',
      refreshToken: 'ref',
      expiresIn: '8h',
      user: { id: '1', email: 'a@b.com', fullName: 'Test', role: 'ADMIN', active: true },
    }
    apiClient.post.mockResolvedValueOnce({ data: loginData })

    const result = await login('a@b.com', 'password123')

    expect(result).toEqual(loginData)
  })

  it('propagates errors (401 toast is handled by the interceptor, not authApi)', async () => {
    // Simulate what the interceptor re-throws after toasting
    const authError = new Error('Request failed with status code 401')
    authError.response = { status: 401, data: { message: 'Credenciales inválidas' } }
    apiClient.post.mockRejectedValueOnce(authError)

    await expect(login('a@b.com', 'wrong-password')).rejects.toThrow()
  })
})

// ─── logout ───────────────────────────────────────────────────────────────────

describe('authApi.logout', () => {
  it('calls POST /api/v1/auth/logout', async () => {
    apiClient.post.mockResolvedValueOnce({ data: {} })

    await logout()

    expect(apiClient.post).toHaveBeenCalledOnce()
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/logout')
  })

  it('calls the logout endpoint even when an accessToken is present in localStorage', async () => {
    // The Authorization header is injected by the request interceptor in apiClient.
    // Here we verify that authApi.logout() always calls the endpoint regardless of
    // whether a token is stored — header injection is tested in the apiClient tests.
    localStorage.setItem('accessToken', 'test-token')
    apiClient.post.mockResolvedValueOnce({ data: {} })

    await logout()

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/logout')
  })

  it('resolves to undefined on success', async () => {
    apiClient.post.mockResolvedValueOnce({ data: { someField: 'ignored' } })

    const result = await logout()

    expect(result).toBeUndefined()
  })
})
