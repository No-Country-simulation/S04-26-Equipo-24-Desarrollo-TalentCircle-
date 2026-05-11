import apiClient from './apiClient'

/**
 * Drafts API module
 * All functions let errors propagate to the apiClient interceptor.
 */

/**
 * Fetch a list of draft summaries with optional filters.
 * Undefined/null filter values are omitted from the query string.
 *
 * @param {{ channel?: string, status?: string, weekStart?: string, weekEnd?: string, page?: number, size?: number }} [filters={}]
 * @returns {Promise<import('../types/api').DraftSummaryDto[]>}
 */
export const list = (filters = {}) => {
  // Build params object, omitting any key whose value is null or undefined
  const params = Object.fromEntries(
    Object.entries(filters).filter(([, v]) => v != null)
  )
  return apiClient.get('/api/v1/drafts', { params }).then((res) => res.data)
}

/**
 * Fetch the full detail of a single draft.
 * @param {string} id
 * @returns {Promise<import('../types/api').DraftDetailDto>}
 */
export const getDetail = (id) =>
  apiClient.get(`/api/v1/drafts/${id}`).then((res) => res.data)

/**
 * Approve a draft.
 * @param {string} id
 * @returns {Promise<import('../types/api').DraftDetailDto>}
 */
export const approve = (id) =>
  apiClient.post(`/api/v1/drafts/${id}/approve`).then((res) => res.data)

/**
 * Reject a draft with a reason.
 * @param {string} id
 * @param {string} reason
 * @returns {Promise<import('../types/api').DraftDetailDto>}
 */
export const reject = (id, reason) =>
  apiClient
    .post(`/api/v1/drafts/${id}/reject`, { reason })
    .then((res) => res.data)

/**
 * Update the editable content of a draft.
 * @param {string} id
 * @param {string} content
 * @returns {Promise<import('../types/api').DraftDetailDto>}
 */
export const updateContent = (id, content) =>
  apiClient
    .patch(`/api/v1/drafts/${id}/content`, { content })
    .then((res) => res.data)

const draftsApi = { list, getDetail, approve, reject, updateContent }
export default draftsApi
