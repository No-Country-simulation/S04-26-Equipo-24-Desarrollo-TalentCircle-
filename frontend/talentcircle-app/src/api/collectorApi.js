import apiClient from './apiClient'

/**
 * Collector API module
 * All functions let errors propagate to the apiClient interceptor.
 */

/**
 * Fetch community activities for a given execution.
 * @param {string} executionId
 * @returns {Promise<import('../types/api').CommunityActivityDto[]>}
 */
export const getActivities = (executionId) =>
  apiClient
    .get('/api/v1/admin/collector/activities', {
      params: { executionId },
    })
    .then((res) => res.data)

const collectorApi = { getActivities }
export default collectorApi
