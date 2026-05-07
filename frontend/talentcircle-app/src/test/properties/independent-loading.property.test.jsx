// Feature: frontend-backend-integration, Property 8: Independent loading states
// Validates: Requirements 9.4

/**
 * Property 8: Independent loading states
 *
 * For any combination of page components rendered simultaneously, setting
 * `loading = true` in one page's local state must not affect the `loading`
 * state of any other page.
 *
 * Each page component owns its loading state as local `useState` — it is NOT
 * stored in Zustand. Therefore, the Drafts page being in a loading state
 * (its fetch never resolves) cannot affect the Admin page's loading state
 * (which follows its own fetch lifecycle).
 *
 * Test strategy (preferred simpler approach from design doc):
 *   1. Mock `draftsApi.list` to return a never-resolving promise (in-flight).
 *   2. Mock `adminApi.getUsers`, `adminApi.getSources`, `adminApi.getConfig`
 *      to resolve immediately with fast-check–generated data.
 *   3. Render both Drafts and Admin simultaneously in the same tree.
 *   4. Assert that Admin's sections show loaded content (no skeleton blocks)
 *      even while Drafts is still loading (skeleton cards still present).
 *   5. fast-check generates the Admin API data, proving independence holds
 *      for any data shape.
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

// ─── Mock adminApi ────────────────────────────────────────────────────────────
vi.mock('../../api/adminApi', () => ({
  default: {
    getUsers:     vi.fn(),
    createUser:   vi.fn(),
    updateUser:   vi.fn(),
    getSources:   vi.fn(),
    createSource: vi.fn(),
    updateSource: vi.fn(),
    getConfig:    vi.fn(),
    updateConfig: vi.fn(),
    getExecutions: vi.fn(),
    triggerExecution: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Drafts.jsx uses the object-destructure pattern: useAppStore()
// Admin.jsx uses the selector pattern:            useAppStore((s) => s.showToast)
// The mock must handle both call signatures.
const mockShowToast = vi.fn()

let storeState = {
  drafts: [],
  setDrafts: (d) => { storeState.drafts = d },
  openModal: vi.fn(),
  updateDraftStatus: vi.fn(),
  showToast: mockShowToast,
}

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn((selector) => {
    // Selector pattern (Admin.jsx): useAppStore((s) => s.showToast)
    if (typeof selector === 'function') {
      return selector(storeState)
    }
    // Object-destructure pattern (Drafts.jsx): useAppStore()
    return storeState
  }),
}))

import draftsApi from '../../api/draftsApi'
import adminApi from '../../api/adminApi'
import { useAppStore } from '../../store/useAppStore'
import Drafts from '../../pages/Drafts/Drafts'
import Admin from '../../pages/Admin/Admin'

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Returns true when the Drafts component is still in the loading state.
 * Loading is indicated by skeleton cards (class containing "skeletonCard")
 * inside the drafts container.
 */
function isDraftsLoading() {
  const container = document.querySelector('[data-testid="drafts-container"]')
  if (!container) return false
  return container.querySelectorAll('[class*="skeletonCard"]').length > 0
}

/**
 * Returns true when the Admin component has finished loading all sections.
 * Admin uses "skeletonBlock" for its per-section skeletons (inside the admin
 * container). When all three sections are done, no skeletonBlock elements
 * remain inside the admin container.
 *
 * Note: Drafts' SkeletonCard also contains skeletonBlock elements, so we must
 * scope this check to the admin container only.
 */
function isAdminDoneLoading() {
  const container = document.querySelector('[data-testid="admin-container"]')
  if (!container) return false
  return container.querySelectorAll('[class*="skeletonBlock"]').length === 0
}

/**
 * Reset the mock store state between iterations so each render starts fresh.
 */
function resetStore() {
  storeState.drafts = []
  storeState.setDrafts = (d) => { storeState.drafts = d }
  storeState.openModal = vi.fn()
  storeState.updateDraftStatus = vi.fn()
  storeState.showToast = mockShowToast

  useAppStore.mockImplementation((selector) => {
    if (typeof selector === 'function') return selector(storeState)
    return storeState
  })
}

// ─── Arbitraries ──────────────────────────────────────────────────────────────

/**
 * Generates a minimal UserDto for Admin's users section.
 */
const userArb = fc.record({
  id:       fc.uuid(),
  fullName: fc.string({ minLength: 1, maxLength: 40 }),
  email:    fc.emailAddress(),
  role:     fc.constantFrom('ADMIN', 'EDITOR'),
  active:   fc.boolean(),
})

/**
 * Generates a minimal SourceDto for Admin's sources section.
 */
const sourceArb = fc.record({
  id:     fc.uuid(),
  name:   fc.string({ minLength: 1, maxLength: 40 }),
  type:   fc.constantFrom('DISCORD', 'CIRCLE', 'SLACK'),
  active: fc.boolean(),
})

/**
 * Generates a minimal ConfigDto for Admin's config section.
 */
const configArb = fc.record({
  llmProvider:        fc.constantFrom('openai', 'anthropic'),
  llmModel:           fc.constantFrom('gpt-4o', 'claude-3-5-sonnet'),
  newsletterPrompt:   fc.string({ maxLength: 80 }),
  linkedinPrompt:     fc.string({ maxLength: 80 }),
  twitterPrompt:      fc.string({ maxLength: 80 }),
  maxItemsPerChannel: fc.integer({ min: 1, max: 100 }),
  scheduleCron:       fc.constant('0 18 * * FRI'),
})

/**
 * Generates the full Admin API payload: users (0–3), sources (0–3), config.
 */
const adminDataArb = fc.record({
  users:   fc.array(userArb,   { minLength: 0, maxLength: 3 }),
  sources: fc.array(sourceArb, { minLength: 0, maxLength: 3 }),
  config:  configArb,
})

// ─── Wrapper component ────────────────────────────────────────────────────────
// Renders both pages side-by-side so we can observe both loading states at once.
function BothPages() {
  return (
    <div>
      <div data-testid="drafts-container">
        <Drafts />
      </div>
      <div data-testid="admin-container">
        <Admin />
      </div>
    </div>
  )
}

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks()
  resetStore()
})

afterEach(() => {
  cleanup()
})

// ─── Property Tests ───────────────────────────────────────────────────────────

describe('Property 8: Independent loading states', () => {
  it(
    'Admin sections finish loading independently while Drafts fetch is still in-flight (any Admin data shape)',
    async () => {
      await fc.assert(
        fc.asyncProperty(adminDataArb, async ({ users, sources, config }) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()

          // Drafts fetch never resolves — simulates a permanently in-flight request.
          // This keeps Drafts in loading=true for the entire test.
          draftsApi.list.mockReturnValue(new Promise(() => {}))

          // Admin fetches resolve immediately with generated data.
          adminApi.getUsers.mockResolvedValue(users)
          adminApi.getSources.mockResolvedValue(sources)
          adminApi.getConfig.mockResolvedValue(config)

          // Act: render both pages simultaneously
          const { unmount } = render(<BothPages />)

          // Assert 1: Drafts is still loading (skeleton cards present)
          // We check this immediately after render — the never-resolving promise
          // guarantees Drafts stays in loading state throughout.
          expect(isDraftsLoading()).toBe(true)

          // Assert 2: Admin finishes loading independently of Drafts.
          // Wait for Admin's three fetches to settle and all skeleton blocks to disappear.
          await waitFor(() => {
            expect(isAdminDoneLoading()).toBe(true)
          }, { timeout: 3000 })

          // Assert 3: Drafts is STILL loading after Admin has finished.
          // This is the core independence check: Admin completing does not
          // affect Drafts' loading state, and Drafts' in-flight state does
          // not prevent Admin from completing.
          expect(isDraftsLoading()).toBe(true)

          // Assert 4: Admin API functions were each called exactly once.
          expect(adminApi.getUsers).toHaveBeenCalledTimes(1)
          expect(adminApi.getSources).toHaveBeenCalledTimes(1)
          expect(adminApi.getConfig).toHaveBeenCalledTimes(1)

          // Assert 5: Drafts API was called (it's just stuck in-flight).
          expect(draftsApi.list).toHaveBeenCalled()

          unmount()
        }),
        { numRuns: 100 }
      )
    },
    60000
  )

  it(
    'Drafts loading state is unaffected by Admin fetch outcome (Admin resolves, Drafts stays loading)',
    async () => {
      await fc.assert(
        fc.asyncProperty(adminDataArb, async ({ users, sources, config }) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()

          // Drafts: never resolves (permanently in-flight)
          draftsApi.list.mockReturnValue(new Promise(() => {}))

          // Admin: all three fetches resolve immediately
          adminApi.getUsers.mockResolvedValue(users)
          adminApi.getSources.mockResolvedValue(sources)
          adminApi.getConfig.mockResolvedValue(config)

          // Act
          const { unmount } = render(<BothPages />)

          // Wait for Admin to fully load
          await waitFor(() => {
            expect(isAdminDoneLoading()).toBe(true)
          }, { timeout: 3000 })

          // The Drafts loading state must remain true — Admin completing
          // its fetch cannot set Drafts' loading to false.
          // Loading state is local useState per component, not shared via Zustand.
          const draftsContainer = document.querySelector('[data-testid="drafts-container"]')
          const draftsSkeletons = draftsContainer
            ? draftsContainer.querySelectorAll('[class*="skeletonCard"]')
            : []
          expect(draftsSkeletons.length).toBeGreaterThan(0)

          unmount()
        }),
        { numRuns: 100 }
      )
    },
    60000
  )

  it(
    'Admin loading state is unaffected by Drafts fetch outcome (Drafts resolves, Admin stays loading)',
    async () => {
      await fc.assert(
        fc.asyncProperty(adminDataArb, async ({ users, sources, config }) => {
          // Arrange
          resetStore()
          vi.clearAllMocks()

          // Drafts: resolves immediately with empty array
          draftsApi.list.mockResolvedValue([])

          // Admin: all three fetches never resolve (permanently in-flight)
          adminApi.getUsers.mockReturnValue(new Promise(() => {}))
          adminApi.getSources.mockReturnValue(new Promise(() => {}))
          adminApi.getConfig.mockReturnValue(new Promise(() => {}))

          // Act
          const { unmount } = render(<BothPages />)

          // Wait for Drafts to finish loading (skeleton cards disappear)
          await waitFor(() => {
            expect(isDraftsLoading()).toBe(false)
          }, { timeout: 3000 })

          // Admin must still be loading — Drafts completing its fetch
          // cannot set Admin's loading states to false.
          const adminContainer = document.querySelector('[data-testid="admin-container"]')
          const adminSkeletons = adminContainer
            ? adminContainer.querySelectorAll('[class*="skeletonBlock"]')
            : []
          expect(adminSkeletons.length).toBeGreaterThan(0)

          unmount()
        }),
        { numRuns: 100 }
      )
    },
    60000
  )
})
