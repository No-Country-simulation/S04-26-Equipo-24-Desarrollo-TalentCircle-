import apiClient from './apiClient'

// ── Executions ───────────────────────────────────────────────────────────────

/** GET /api/v1/admin/executions */
export const getExecutions = async () => {
  const { data } = await apiClient.get('/admin/executions')
  return data
}

/** GET /api/v1/admin/executions/:id */
export const getExecutionDetail = async (id) => {
  const { data } = await apiClient.get(`/admin/executions/${id}`)
  return data
}

/** POST /api/v1/admin/executions/trigger */
export const triggerExecution = async (triggeredBy) => {
  await apiClient.post('/admin/executions/trigger', null, { params: { triggeredBy } })
}

// ── Sources ──────────────────────────────────────────────────────────────────

/** GET /api/v1/admin/sources */
export const getSources = async () => {
  const { data } = await apiClient.get('/admin/sources')
  return data
}

/** POST /api/v1/admin/sources */
export const createSource = async (source) => {
  const { data } = await apiClient.post('/admin/sources', source)
  return data
}

/** PUT /api/v1/admin/sources/:id */
export const updateSource = async (id, source) => {
  const { data } = await apiClient.put(`/admin/sources/${id}`, source)
  return data
}

/** DELETE /api/v1/admin/sources/:id */
export const deleteSource = async (id) => {
  await apiClient.delete(`/admin/sources/${id}`)
}

// ── Config ───────────────────────────────────────────────────────────────────

/** GET /api/v1/admin/config */
export const getConfig = async () => {
  const { data } = await apiClient.get('/admin/config')
  return data
}

/** PUT /api/v1/admin/config */
export const updateConfig = async (config) => {
  const { data } = await apiClient.put('/admin/config', config)
  return data
}

// ── Users ────────────────────────────────────────────────────────────────────

/** GET /api/v1/admin/users */
export const getAdminUsers = async () => {
  const { data } = await apiClient.get('/admin/users')
  return data
}

/** POST /api/v1/admin/users */
export const createAdminUser = async (user) => {
  const { data } = await apiClient.post('/admin/users', user)
  return data
}

/** PUT /api/v1/admin/users/:id */
export const updateAdminUser = async (id, user) => {
  const { data } = await apiClient.put(`/admin/users/${id}`, user)
  return data
}
