import apiClient from './apiClient'

/** GET /api/v1/admin/executions — última ejecución para el banner */
export const getLatestExecution = async () => {
  const { data } = await apiClient.get('/admin/executions')
  // Ordenar por fecha y devolver la más reciente
  if (!data?.length) return null
  return data.sort((a, b) => new Date(b.startedAt ?? 0) - new Date(a.startedAt ?? 0))[0]
}

/** GET /api/v1/drafts — borradores de la semana actual */
export const getWeekDrafts = async () => {
  const { data } = await apiClient.get('/drafts', { params: { size: 10 } })
  return data
}

/** GET /api/v1/admin/collector/activities — actividades recientes */
export const getRecentActivities = async (executionId) => {
  const { data } = await apiClient.get('/admin/collector/activities', {
    params: { executionId },
  })
  return data
}
