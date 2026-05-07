/**
 * Unit tests for src/api/draftsApi.js
 * Requirements: 4.1, 4.4–4.8
 *
 * apiClient is mocked so no real HTTP calls are made.
 * Error toasting is handled by the apiClient interceptor — not tested here.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the apiClient module before importing draftsApi
vi.mock('../apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

import apiClient from '../apiClient'
import { list, approve, reject, updateContent } from '../draftsApi'

beforeEach(() => {
  vi.clearAllMocks()
})

// ─── list ─────────────────────────────────────────────────────────────────────

describe('draftsApi.list', () => {
  it('calls GET /api/v1/drafts with no query params when no filters are provided', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list()

    expect(apiClient.get).toHaveBeenCalledOnce()
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/drafts', { params: {} })
  })

  it('calls GET /api/v1/drafts with no query params when an empty object is passed', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list({})

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/drafts', { params: {} })
  })

  it('includes channel and status params when both filters are provided', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list({ channel: 'LINKEDIN', status: 'PENDING' })

    const [url, config] = apiClient.get.mock.calls[0]
    expect(url).toBe('/api/v1/drafts')
    expect(config.params).toEqual({ channel: 'LINKEDIN', status: 'PENDING' })
  })

  it('includes only channel param when only channel filter is provided', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list({ channel: 'NEWSLETTER' })

    const [, config] = apiClient.get.mock.calls[0]
    expect(config.params).toEqual({ channel: 'NEWSLETTER' })
    expect(config.params).not.toHaveProperty('status')
  })

  it('includes only status param when only status filter is provided', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list({ status: 'APPROVED' })

    const [, config] = apiClient.get.mock.calls[0]
    expect(config.params).toEqual({ status: 'APPROVED' })
    expect(config.params).not.toHaveProperty('channel')
  })

  it('omits null and undefined filter values', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [] })

    await list({ channel: null, status: undefined, page: 0 })

    const [, config] = apiClient.get.mock.calls[0]
    expect(config.params).toEqual({ page: 0 })
    expect(config.params).not.toHaveProperty('channel')
    expect(config.params).not.toHaveProperty('status')
  })

  it('returns the response data array', async () => {
    const drafts = [
      { id: '1', channel: 'LINKEDIN', status: 'PENDING', createdAt: '2024-01-01', summary: 'Test' },
    ]
    apiClient.get.mockResolvedValueOnce({ data: drafts })

    const result = await list()

    expect(result).toEqual(drafts)
  })
})

// ─── approve ──────────────────────────────────────────────────────────────────

describe('draftsApi.approve', () => {
  it('calls POST /api/v1/drafts/{id}/approve with the correct endpoint', async () => {
    const draftDetail = { id: 'abc-123', status: 'APPROVED' }
    apiClient.post.mockResolvedValueOnce({ data: draftDetail })

    await approve('abc-123')

    expect(apiClient.post).toHaveBeenCalledOnce()
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/drafts/abc-123/approve')
  })

  it('returns the updated draft detail on success', async () => {
    const draftDetail = { id: 'abc-123', status: 'APPROVED', channel: 'LINKEDIN' }
    apiClient.post.mockResolvedValueOnce({ data: draftDetail })

    const result = await approve('abc-123')

    expect(result).toEqual(draftDetail)
  })

  it('uses the provided id in the URL', async () => {
    apiClient.post.mockResolvedValueOnce({ data: {} })

    await approve('draft-xyz-999')

    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/drafts/draft-xyz-999/approve')
  })
})

// ─── reject ───────────────────────────────────────────────────────────────────

describe('draftsApi.reject', () => {
  it('calls POST /api/v1/drafts/{id}/reject with the correct endpoint and reason payload', async () => {
    const draftDetail = { id: 'abc-123', status: 'REJECTED' }
    apiClient.post.mockResolvedValueOnce({ data: draftDetail })

    await reject('abc-123', 'Rechazado desde el panel')

    expect(apiClient.post).toHaveBeenCalledOnce()
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/drafts/abc-123/reject', {
      reason: 'Rechazado desde el panel',
    })
  })

  it('sends the reason in the request body', async () => {
    apiClient.post.mockResolvedValueOnce({ data: {} })

    await reject('draft-42', 'Contenido inapropiado')

    const [, payload] = apiClient.post.mock.calls[0]
    expect(payload).toEqual({ reason: 'Contenido inapropiado' })
  })

  it('returns the updated draft detail on success', async () => {
    const draftDetail = { id: 'abc-123', status: 'REJECTED', channel: 'TWITTER' }
    apiClient.post.mockResolvedValueOnce({ data: draftDetail })

    const result = await reject('abc-123', 'some reason')

    expect(result).toEqual(draftDetail)
  })

  it('uses the provided id in the URL', async () => {
    apiClient.post.mockResolvedValueOnce({ data: {} })

    await reject('draft-xyz-999', 'reason')

    const [url] = apiClient.post.mock.calls[0]
    expect(url).toBe('/api/v1/drafts/draft-xyz-999/reject')
  })
})

// ─── updateContent ────────────────────────────────────────────────────────────

describe('draftsApi.updateContent', () => {
  it('calls PATCH /api/v1/drafts/{id}/content with the correct endpoint and content payload', async () => {
    const draftDetail = { id: 'abc-123', content: 'Updated content' }
    apiClient.patch.mockResolvedValueOnce({ data: draftDetail })

    await updateContent('abc-123', 'Updated content')

    expect(apiClient.patch).toHaveBeenCalledOnce()
    expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/drafts/abc-123/content', {
      content: 'Updated content',
    })
  })

  it('sends the content in the request body', async () => {
    apiClient.patch.mockResolvedValueOnce({ data: {} })

    await updateContent('draft-42', 'New newsletter text here')

    const [, payload] = apiClient.patch.mock.calls[0]
    expect(payload).toEqual({ content: 'New newsletter text here' })
  })

  it('returns the updated draft detail on success', async () => {
    const draftDetail = { id: 'abc-123', editedContent: 'Updated content', status: 'PENDING' }
    apiClient.patch.mockResolvedValueOnce({ data: draftDetail })

    const result = await updateContent('abc-123', 'Updated content')

    expect(result).toEqual(draftDetail)
  })

  it('uses the provided id in the URL', async () => {
    apiClient.patch.mockResolvedValueOnce({ data: {} })

    await updateContent('draft-xyz-999', 'some content')

    const [url] = apiClient.patch.mock.calls[0]
    expect(url).toBe('/api/v1/drafts/draft-xyz-999/content')
  })
})
