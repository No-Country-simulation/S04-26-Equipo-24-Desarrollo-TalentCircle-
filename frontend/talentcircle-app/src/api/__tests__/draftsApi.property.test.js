/**
 * Property-based tests for src/api/draftsApi.js
 *
 * Property 9: Filter parameters passed to API
 * For any valid combination of channel (NEWSLETTER, LINKEDIN, TWITTER, or absent)
 * and status (PENDING, APPROVED, REJECTED, PUBLISHED, or absent), the resulting
 * GET call must include exactly those values as query params and omit unset params.
 *
 * Validates: Requirements 4.4
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// Mock apiClient before importing draftsApi
vi.mock('../apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

import apiClient from '../apiClient'
import { list } from '../draftsApi'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('Property 9: Filter parameters passed to API', () => {
  it('passes exactly the provided channel and status params, omitting absent ones', async () => {
    // fc.option produces null when absent (with { nil: null })
    const channelArb = fc.option(
      fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
      { nil: null }
    )
    const statusArb = fc.option(
      fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED'),
      { nil: null }
    )

    await fc.assert(
      fc.asyncProperty(channelArb, statusArb, async (channel, status) => {
        apiClient.get.mockResolvedValueOnce({ data: [] })

        // Build the filters object as the caller would — null values are passed in
        const filters = {}
        if (channel !== null) filters.channel = channel
        if (status !== null) filters.status = status

        await list(filters)

        expect(apiClient.get).toHaveBeenCalledOnce()

        const [url, config] = apiClient.get.mock.calls[0]
        expect(url).toBe('/api/v1/drafts')

        const params = config.params

        // If channel was provided, it must appear in params with the exact value
        if (channel !== null) {
          expect(params).toHaveProperty('channel', channel)
        } else {
          // If channel was absent, it must NOT appear in params
          expect(params).not.toHaveProperty('channel')
        }

        // If status was provided, it must appear in params with the exact value
        if (status !== null) {
          expect(params).toHaveProperty('status', status)
        } else {
          // If status was absent, it must NOT appear in params
          expect(params).not.toHaveProperty('status')
        }

        vi.clearAllMocks()
      }),
      { numRuns: 100 }
    )
  })

  it('passes null channel and status values through fc.option and strips them from params', async () => {
    // Explicitly test the null-stripping behaviour using fc.option which can produce null
    const channelArb = fc.option(
      fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
      { nil: null }
    )
    const statusArb = fc.option(
      fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED'),
      { nil: null }
    )

    await fc.assert(
      fc.asyncProperty(channelArb, statusArb, async (channel, status) => {
        apiClient.get.mockResolvedValueOnce({ data: [] })

        // Pass null values directly — list() must strip them
        await list({ channel, status })

        const [, config] = apiClient.get.mock.calls[0]
        const params = config.params

        // Null values must never appear in the params sent to apiClient
        const paramValues = Object.values(params)
        expect(paramValues).not.toContain(null)
        expect(paramValues).not.toContain(undefined)

        // Present (non-null) values must match exactly
        if (channel !== null) {
          expect(params.channel).toBe(channel)
        }
        if (status !== null) {
          expect(params.status).toBe(status)
        }

        vi.clearAllMocks()
      }),
      { numRuns: 100 }
    )
  })
})
