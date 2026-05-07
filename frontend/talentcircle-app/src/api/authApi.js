import apiClient from './apiClient'

/**
 * Auth API module
 * All functions let errors propagate to the apiClient interceptor.
 */

/**
 * Authenticate a user with email and password.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<import('../types/api').LoginResponse>}
 */
export const login = (email, password) =>
  apiClient
    .post('/api/v1/auth/login', { email, password })
    .then((res) => res.data)

/**
 * Invalidate the current session on the server.
 * The Authorization header is attached automatically by the request interceptor.
 * @returns {Promise<void>}
 */
export const logout = () =>
  apiClient.post('/api/v1/auth/logout').then(() => undefined)

/**
 * Exchange a refresh token for a new access token.
 * @param {string} refreshToken
 * @returns {Promise<import('../types/api').LoginResponse>}
 */
export const refresh = (refreshToken) =>
  apiClient
    .post('/api/v1/auth/refresh', { refreshToken })
    .then((res) => res.data)

const authApi = { login, logout, refresh }
export default authApi
