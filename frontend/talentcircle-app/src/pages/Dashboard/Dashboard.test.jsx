/**
 * Component tests for the Dashboard page.
 * Task 11.3 — Requirements: 7.1–7.7, 9.1
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import Dashboard from './Dashboard'

// ─── Mock react-router-dom ────────────────────────────────────────────────────
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}))

// ─── Mock API modules ─────────────────────────────────────────────────────────
vi.mock('../../api/draftsApi', () => ({
  default: { list: vi.fn() },
}))

vi.mock('../../api/adminApi', () => ({
  default: { getExecutions: vi.fn() },
}))

vi.mock('../../api/collectorApi', () => ({
  default: { getActivities: vi.fn() },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
const mockOpenModal = vi.fn()

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn((selector) => {
    if (selector) return selector({ openModal: mockOpenModal })
    return { openModal: mockOpenModal }
  }),
}))

import draftsApi from '../../api/draftsApi'
import adminApi from '../../api/adminApi'
import collectorApi from '../../api/collectorApi'
import { useAppStore } from '../../store/useAppStore'

// ─── Sample data ──────────────────────────────────────────────────────────────
const SAMPLE_DRAFTS = [
  { id: 'd1', channel: 'NEWSLETTER', status: 'PENDING',   createdAt: '2025-04-25T10:00:00Z', summary: 'Newsletter draft one',   aiScore: 8.5 },
  { id: 'd2', channel: 'LINKEDIN',   status: 'APPROVED',  createdAt: '2025-04-24T09:00:00Z', summary: 'LinkedIn draft two',     aiScore: 7.2 },
  { id: 'd3', channel: 'TWITTER',    status: 'PENDING',   createdAt: '2025-04-23T08:00:00Z', summary: 'Twitter draft three',    aiScore: 6.0 },
  { id: 'd4', channel: 'NEWSLETTER', status: 'PUBLISHED', createdAt: '2025-04-22T07:00:00Z', summary: 'Newsletter draft four',  aiScore: 9.1 },
]

const SAMPLE_EXECUTIONS = [
  { id: 'exec-old', weekStart: '2025-04-14', weekEnd: '2025-04-20', status: 'COMPLETED', startedAt: '2025-04-14T08:00:00Z', completedAt: '2025-04-14T09:00:00Z' },
  { id: 'exec-new', weekStart: '2025-04-21', weekEnd: '2025-04-27', status: 'COMPLETED', startedAt: '2025-04-21T08:00:00Z', completedAt: '2025-04-21T09:30:00Z' },
]

const SAMPLE_ACTIVITIES = [
  { id: 'act-1', title: 'Great post', content: 'Some content here', type: 'POST', reactionCount: 12, responseCount: 3, shareCount: 5, author: 'Alice', sourceUrl: 'https://example.com/1' },
  { id: 'act-2', title: 'Question asked', content: 'How does this work?', type: 'QUESTION', reactionCount: 4, responseCount: 8, shareCount: 1, author: 'Bob', sourceUrl: 'https://example.com/2' },
]

beforeEach(() => {
  vi.clearAllMocks()

  // Re-apply selector mock after clearAllMocks
  useAppStore.mockImplementation((selector) => {
    if (selector) return selector({ openModal: mockOpenModal })
    return { openModal: mockOpenModal }
  })

  // Default: all fetches resolve with sample data
  draftsApi.list.mockResolvedValue(SAMPLE_DRAFTS)
  adminApi.getExecutions.mockResolvedValue(SAMPLE_EXECUTIONS)
  collectorApi.getActivities.mockResolvedValue(SAMPLE_ACTIVITIES)
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ─── Helper: wait for stats and feed to finish loading ────────────────────────
async function waitForLoad() {
  await waitFor(() => {
    // Stats grid should show derived values (not skeletons)
    expect(screen.getByText('4')).toBeInTheDocument() // generatedCount = 4 drafts
  })
}

// ─── Test 1: Skeletons render while fetches are in flight ─────────────────────
it('renders skeleton blocks while fetches are in flight', () => {
  // Never resolve — keeps loading=true
  draftsApi.list.mockReturnValue(new Promise(() => {}))
  adminApi.getExecutions.mockReturnValue(new Promise(() => {}))
  collectorApi.getActivities.mockReturnValue(new Promise(() => {}))

  render(<Dashboard />)

  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBeGreaterThan(0)
})

// ─── Test 2: Stat cards display correct derived values ────────────────────────
it('displays correct "Borradores Generados" count from drafts.length', async () => {
  render(<Dashboard />)

  await waitForLoad()

  // 4 drafts total → "Borradores Generados" = 4
  const statsGrid = screen.getByTestId('stats-grid')
  expect(statsGrid).toHaveTextContent('4')
})

it('displays correct "Pendientes de Revisión" count from PENDING drafts', async () => {
  render(<Dashboard />)

  await waitForLoad()

  // 2 PENDING drafts (d1, d3)
  const statsGrid = screen.getByTestId('stats-grid')
  expect(statsGrid).toHaveTextContent('2')
})

// ─── Test 3: collectorApi.getActivities called with most recent execution id ──
it('calls getActivities with the most recent execution id', async () => {
  render(<Dashboard />)

  await waitFor(() => {
    expect(collectorApi.getActivities).toHaveBeenCalledWith('exec-new')
  })
})

it('does NOT call getActivities with the older execution id', async () => {
  render(<Dashboard />)

  await waitFor(() => {
    expect(collectorApi.getActivities).toHaveBeenCalledTimes(1)
    expect(collectorApi.getActivities).not.toHaveBeenCalledWith('exec-old')
  })
})

// ─── Test 4: Activity feed renders CommunityActivityDto fields ────────────────
it('renders activity author, reactionCount, responseCount, shareCount', async () => {
  render(<Dashboard />)

  await waitFor(() => {
    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
  })

  const feed = screen.getByTestId('activity-feed')
  // Reaction / response / share counts
  expect(feed).toHaveTextContent('12') // reactionCount for act-1
  expect(feed).toHaveTextContent('3')  // responseCount for act-1
  expect(feed).toHaveTextContent('5')  // shareCount for act-1
})

// ─── Test 5: Three most recent drafts shown in "Borradores de la Semana" ──────
it('shows at most 3 drafts in the draft list, sorted by createdAt descending', async () => {
  render(<Dashboard />)

  await waitFor(() => {
    const draftList = screen.getByTestId('draft-list')
    // The three most recent: d1 (Apr 25), d2 (Apr 24), d3 (Apr 23)
    expect(draftList).toHaveTextContent('Newsletter draft one')
    expect(draftList).toHaveTextContent('LinkedIn draft two')
    expect(draftList).toHaveTextContent('Twitter draft three')
    // d4 (Apr 22) should NOT appear
    expect(draftList).not.toHaveTextContent('Newsletter draft four')
  })
})

// ─── Test 6: No activities call when executions array is empty ────────────────
it('does not call getActivities when executions list is empty', async () => {
  adminApi.getExecutions.mockResolvedValue([])

  render(<Dashboard />)

  await waitFor(() => {
    // Stats should have loaded (drafts resolved)
    expect(screen.getByTestId('stats-grid')).toBeInTheDocument()
  })

  // Give a tick for any potential async calls
  await new Promise((r) => setTimeout(r, 50))

  expect(collectorApi.getActivities).not.toHaveBeenCalled()
})

// ─── Test 7: Skeletons removed after successful load ─────────────────────────
it('removes all skeleton blocks after data loads', async () => {
  render(<Dashboard />)

  await waitForLoad()

  // Feed also needs to load
  await waitFor(() => {
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })

  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBe(0)
})

// ─── Test 8: Skeletons removed after failed load ──────────────────────────────
it('removes skeleton blocks after fetch error', async () => {
  draftsApi.list.mockRejectedValue(new Error('Network error'))
  adminApi.getExecutions.mockRejectedValue(new Error('Network error'))

  render(<Dashboard />)

  await waitFor(() => {
    const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
    expect(skeletonBlocks.length).toBe(0)
  })
})
