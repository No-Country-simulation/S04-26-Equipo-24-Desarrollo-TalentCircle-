import apiClient from './apiClient'

/**
 * Admin API module
 * All functions let errors propagate to the apiClient interceptor.
 */

// ─── Users ────────────────────────────────────────────────────────────────────

/**
 * Fetch all users.
 * @returns {Promise<import('../types/api').UserDto[]>}
 */
export const getUsers = () =>
  apiClient.get('/api/v1/admin/users').then((res) => res.data)

/**
 * Create a new user.
 * @param {Partial<import('../types/api').UserDto>} data
 * @returns {Promise<import('../types/api').UserDto>}
 */
export const createUser = (data) =>
  apiClient.post('/api/v1/admin/users', data).then((res) => res.data)

/**
 * Update an existing user.
 * @param {string} id
 * @param {Partial<import('../types/api').UserDto>} data
 * @returns {Promise<import('../types/api').UserDto>}
 */
export const updateUser = (id, data) =>
  apiClient.put(`/api/v1/admin/users/${id}`, data).then((res) => res.data)

// ─── Sources ──────────────────────────────────────────────────────────────────

/**
 * Fetch all community sources.
 * @returns {Promise<import('../types/api').SourceDto[]>}
 */
export const getSources = () =>
  apiClient.get('/api/v1/admin/sources').then((res) => res.data)

/**
 * Create a new community source.
 * @param {Partial<import('../types/api').SourceDto>} data
 * @returns {Promise<import('../types/api').SourceDto>}
 */
export const createSource = (data) =>
  apiClient.post('/api/v1/admin/sources', data).then((res) => res.data)

/**
 * Update an existing community source.
 * @param {string} id
 * @param {Partial<import('../types/api').SourceDto>} data
 * @returns {Promise<import('../types/api').SourceDto>}
 */
export const updateSource = (id, data) =>
  apiClient.put(`/api/v1/admin/sources/${id}`, data).then((res) => res.data)

// ─── Pipeline Config ──────────────────────────────────────────────────────────

/**
 * Fetch the current pipeline configuration.
 * @returns {Promise<import('../types/api').ConfigDto>}
 */
export const getConfig = () =>
  apiClient.get('/api/v1/admin/config').then((res) => res.data)

/**
 * Replace the pipeline configuration.
 * @param {import('../types/api').ConfigDto} data
 * @returns {Promise<import('../types/api').ConfigDto>}
 */
export const updateConfig = (data) =>
  apiClient.put('/api/v1/admin/config', data).then((res) => res.data)

// ─── Executions ───────────────────────────────────────────────────────────────

/**
 * Fetch all pipeline execution summaries.
 * @returns {Promise<import('../types/api').ExecutionSummaryDto[]>}
 */
export const getExecutions = () =>
  apiClient.get('/api/v1/admin/executions').then((res) => res.data)

/**
 * Trigger a manual pipeline execution.
 * @param {string} email - Email of the user triggering the execution.
 * @returns {Promise<void>} Resolves on HTTP 202.
 */
export const triggerExecution = (email) =>
  apiClient
    .post(`/api/v1/admin/executions/trigger?triggeredBy=${encodeURIComponent(email)}`)
    .then(() => undefined)

const adminApi = {
  getUsers,
  createUser,
  updateUser,
  getSources,
  createSource,
  updateSource,
  getConfig,
  updateConfig,
  getExecutions,
  triggerExecution,
}
export default adminApi
