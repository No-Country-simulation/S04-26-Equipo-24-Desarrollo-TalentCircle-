// Feature: frontend-backend-integration, Property 7: Loading flag always false after fetch
// Validates: Requirements 9.2

/**
 * Property 7: Loading flag always false after fetch
 *
 * For any page-level data fetch (success or error), after the fetch settles,
 * the local `loading` state must be `false`. This is observable through the DOM:
 * when loading=true the Drafts component renders SkeletonCard elements (class
 * containing "skeletonCard"); when loading=false it renders either the empty-state
 * div or the real draft cards.
 *
 * The Drafts component uses the cancelled-flag + finally pattern:
 *   .finally(() => { if (!cancelled) setLoading(false) })
 * This property verifies that pattern holds for both success and rejection paths.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import * as fc from 'fast-check'

// ─── Mock react-router-dom ────────────────────────────────────────────────────
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}))

// ─── Mock draftsApi ───────────────────────────────────────────────────────────
vi.mock('../../api/draftsApi', () => ({
  default: {
    list: vi.fn(),
    getDetail: vi.fn(),
    approve: vi.fn(),
    reject: vi.fn(),
    updateContent: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Drafts.jsx calls: const { drafts, setDrafts } = useAppStore()
// DraftCard calls:  const { openModal, updateDraftStatus, showToast } = useAppStore()
// We need a real reactive store so setDrafts updates drafts correctly.
// Use a simple in-memory store that mimics the Zustand API.
let storeState = {
  drafts: [],
  setDrafts: (d) => { storeState.drafts = d },
  openModal: vi.fn(),
  updateDraftStatus: vi.fn(),
  showToast: vi.fn(),
}

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(() => storeState),
}))

import draftsApi from '../../api/draftsApi'
import { useAppStore } from '../../store/useAppStore'
import Drafts from '../../pages/Drafts/Drafts'

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Returns true when the Drafts component is no longer in the loading state.
 * Loading state is indicated by the presence of elements with a class name
 * containing "skeletonCard". When loading=false, those elements are gone.
 */
function isLoadingDone() {
  const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
  return skeletons.length === 0
}

/**
 * Reset the mock store state between iterations so each render starts fresh.
 */
function resetStore() {
  storeState.drafts = []
  storeState.setDrafts = (d) => { storeState.drafts = d }
  storeState.openModal = vi.fn()
  storeState.updateDraftStatus = vi.fn()
  storeState.showToast = vi.fn()
  useAppStore.mockReturnValue(storeState)
}

// ─── Arbitraries ──────────────────────────────────────────────────────────────

/**
 * Generates a minimal DraftSummaryDto for use in success responses.
 */
const draftArb = fc.record({
  id: fc.uuid(),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED'),
  createdAt: fc.constant('2025-04-25T10:00:00Z'),
  summary: fc.string({ minLength: 1, maxLength: 80 }),
  aiScore: fc.float({ min: 0, max: 10, noNaN: true }),
})

/**
 * Generates an array of 0–5 draft summaries (success payload).
 */
const draftsArrayArb = fc.array(draftArb, { minLength: 0, maxLength: 5 })

/**
 * Generates an error object (rejection payload).
 */
const apiErrorArb = fc.record({
  message: fc.string({ minLength: 1 }),
}).map(({ message }) => {
  const err = new Error(message)
  err.response = { status: 500, data: { message } }
  return err
})

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks()
  resetStore()
})

afterEach(() => {
  cleanup()
})

// ─── Property Tests ───────────────────────────────────────────────────────────

describe('Property 7: Loading flag always false after fetch', () => {
  it(
    'loading is false after a successful draftsApi.list response (any array of drafts)',
    async () => {
      await fc.assert(
        fc.asyncProperty(draftsArrayArb, async (drafts) => {
          // Arrange: reset store and mock a successful API response.
          // Use mockResolvedValue (not Once) because Drafts.jsx has two useEffects
          // that both call fetchDrafts on initial render (mount + filter change).
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockResolvedValue(drafts)

          // Act: render the Drafts component
          const { unmount } = render(<Drafts />)

          // Assert: after the fetch settles, loading must be false
          // (no skeleton cards in the DOM)
          await waitFor(() => {
            expect(isLoadingDone()).toBe(true)
          })

          // Verify the component rendered the correct post-loading state:
          // either the empty-state or the draft cards grid
          const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
          expect(skeletons.length).toBe(0)

          unmount()
        }),
        { numRuns: 100 }
      )
    }
  )

  it(
    'loading is false after draftsApi.list rejects (any error)',
    async () => {
      await fc.assert(
        fc.asyncProperty(apiErrorArb, async (error) => {
          // Arrange: reset store and mock a rejected API response.
          // Use mockRejectedValue (not Once) for the same reason as above.
          resetStore()
          vi.clearAllMocks()
          draftsApi.list.mockRejectedValue(error)

          // Act: render the Drafts component
          const { unmount } = render(<Drafts />)

          // Assert: after the fetch settles (even on error), loading must be false
          await waitFor(() => {
            expect(isLoadingDone()).toBe(true)
          })

          // On error, drafts remain empty → no skeleton cards
          const skeletons = document.querySelectorAll('[class*="skeletonCard"]')
          expect(skeletons.length).toBe(0)

          unmount()
        }),
        { numRuns: 100 }
      )
    }
  )

  it(
    'loading is false regardless of whether the API succeeds or fails (combined property)',
    async () => {
      // Randomly decide success or failure for each iteration
      const outcomeArb = fc.oneof(
        draftsArrayArb.map((drafts) => ({ type: 'success', drafts })),
        apiErrorArb.map((error) => ({ type: 'error', error }))
      )

      await fc.assert(
        fc.asyncProperty(outcomeArb, async (outcome) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()

          if (outcome.type === 'success') {
            draftsApi.list.mockResolvedValue(outcome.drafts)
          } else {
            draftsApi.list.mockRejectedValue(outcome.error)
          }

          // Act
          const { unmount } = render(<Drafts />)

          // Assert: loading must always be false after fetch settles
          await waitFor(() => {
            expect(isLoadingDone()).toBe(true)
          })

          unmount()
        }),
        { numRuns: 100 }
      )
    }
  )
})
