/**
 * Component tests for the Drafts page.
 * Task 8.3 — Requirements: 4.2–4.6, 9.1, 9.3
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
import Drafts from './Drafts'

// ─── Mock draftsApi ───────────────────────────────────────────────────────────
vi.mock('../../api/draftsApi', () => ({
  default: {
    list:          vi.fn(),
    getDetail:     vi.fn(),
    approve:       vi.fn(),
    reject:        vi.fn(),
    updateContent: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Drafts.jsx uses the destructuring pattern:
//   const { drafts, setDrafts } = useAppStore()          ← page component
//   const { openModal, updateDraftStatus, showToast } = useAppStore()  ← DraftCard
//
// We need a reactive in-memory store so setDrafts actually updates drafts
// and the component re-renders correctly.
let storeState

function makeStore() {
  const state = {
    drafts: [],
    openModal: vi.fn(),
    updateDraftStatus: vi.fn(),
    showToast: vi.fn(),
  }
  state.setDrafts = (d) => {
    state.drafts = d
    // Notify the mock so subsequent useAppStore() calls return updated drafts
    useAppStore.mockReturnValue(state)
  }
  return state
}

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(),
}))

import draftsApi from '../../api/draftsApi'
import { useAppStore } from '../../store/useAppStore'

// ─── Sample data ──────────────────────────────────────────────────────────────
const PENDING_DRAFT = {
  id: 'draft-1',
  channel: 'NEWSLETTER',
  status: 'PENDING',
  createdAt: '2025-04-25T10:00:00Z',
  summary: 'This is a newsletter draft summary.',
  aiScore: 8.5,
}

const APPROVED_DRAFT = {
  id: 'draft-2',
  channel: 'LINKEDIN',
  status: 'APPROVED',
  createdAt: '2025-04-24T09:00:00Z',
  summary: 'This is a LinkedIn draft summary.',
  aiScore: 7.2,
}

// ─── Setup / Teardown ─────────────────────────────────────────────────────────
beforeEach(() => {
  vi.clearAllMocks()
  storeState = makeStore()
  useAppStore.mockReturnValue(storeState)
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ─── Test 1: Skeleton renders while loading === true ─────────────────────────
// Validates: Requirements 4.2, 9.1
it('renders skeleton cards while loading is true', () => {
  // Never resolves — keeps loading=true for the duration of this test
  draftsApi.list.mockReturnValue(new Promise(() => {}))

  render(<Drafts />)

  // The SkeletonCard component renders elements with class containing "skeletonCard"
  const skeletonCards = document.querySelectorAll('[class*="skeletonCard"]')
  expect(skeletonCards.length).toBeGreaterThan(0)

  // Real draft content should NOT be visible while loading
  expect(screen.queryByText(PENDING_DRAFT.summary)).not.toBeInTheDocument()
})

// ─── Test 2: Skeleton disappears after data loads ─────────────────────────────
// Validates: Requirements 9.1, 9.2
it('removes skeleton cards after data loads', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])

  render(<Drafts />)

  // Initially skeletons are present
  expect(document.querySelectorAll('[class*="skeletonCard"]').length).toBeGreaterThan(0)

  // After fetch settles, skeletons should be gone
  await waitFor(() => {
    expect(document.querySelectorAll('[class*="skeletonCard"]').length).toBe(0)
  })
})

// ─── Test 3: Empty state renders when API returns [] ─────────────────────────
// Validates: Requirements 4.2, 9.2
it('renders empty state when API returns an empty array', async () => {
  draftsApi.list.mockResolvedValue([])

  render(<Drafts />)

  await waitFor(() => {
    // The empty-state div contains this text
    expect(
      screen.getByText('No hay borradores que coincidan con los filtros')
    ).toBeInTheDocument()
  })

  // No skeleton cards should remain
  expect(document.querySelectorAll('[class*="skeletonCard"]').length).toBe(0)
})

// ─── Test 4: Draft cards render when API returns data ────────────────────────
// Validates: Requirements 4.1
it('renders draft cards after a successful fetch', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT, APPROVED_DRAFT])

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
    expect(screen.getByText(APPROVED_DRAFT.summary)).toBeInTheDocument()
  })
})

// ─── Test 5: Filter chip change triggers draftsApi.list with correct params ──
// Validates: Requirements 4.4
//
// Note: Drafts.jsx has two useEffects — one for mount and one for [channel, status].
// Both fire on initial render, so draftsApi.list is called twice on mount.
// We wait for the empty-state to confirm loading is done, then clear mocks and
// click the filter chip to verify the next call uses the correct params.
it('calls draftsApi.list with channel param when a channel filter chip is clicked', async () => {
  draftsApi.list.mockResolvedValue([])

  render(<Drafts />)

  // Wait for initial load to finish (empty state visible = loading done)
  await waitFor(() => {
    expect(screen.getByText('No hay borradores que coincidan con los filtros')).toBeInTheDocument()
  })

  vi.clearAllMocks()
  draftsApi.list.mockResolvedValue([])

  // Click the NEWSLETTER channel chip
  fireEvent.click(screen.getByRole('button', { name: 'Newsletter' }))

  await waitFor(() => {
    expect(draftsApi.list).toHaveBeenCalledWith({ channel: 'NEWSLETTER' })
  })
})

// ─── Test 6: Status filter chip change triggers draftsApi.list with correct params
// Validates: Requirements 4.4
it('calls draftsApi.list with status param when a status filter chip is clicked', async () => {
  draftsApi.list.mockResolvedValue([])

  render(<Drafts />)

  // Wait for initial load to finish
  await waitFor(() => {
    expect(screen.getByText('No hay borradores que coincidan con los filtros')).toBeInTheDocument()
  })

  vi.clearAllMocks()
  draftsApi.list.mockResolvedValue([])

  // Click the PENDING status chip
  fireEvent.click(screen.getByRole('button', { name: 'Pendiente' }))

  await waitFor(() => {
    expect(draftsApi.list).toHaveBeenCalledWith({ status: 'PENDING' })
  })
})

// ─── Test 7: Both channel and status filters are passed together ──────────────
// Validates: Requirements 4.4
it('calls draftsApi.list with both channel and status params when both filters are set', async () => {
  draftsApi.list.mockResolvedValue([])

  render(<Drafts />)

  // Wait for initial load
  await waitFor(() => {
    expect(screen.getByText('No hay borradores que coincidan con los filtros')).toBeInTheDocument()
  })

  vi.clearAllMocks()
  draftsApi.list.mockResolvedValue([])

  // Set channel filter first
  fireEvent.click(screen.getByRole('button', { name: 'LinkedIn' }))

  await waitFor(() => {
    expect(draftsApi.list).toHaveBeenCalledWith({ channel: 'LINKEDIN' })
  })

  vi.clearAllMocks()
  draftsApi.list.mockResolvedValue([])

  // Now also set status filter
  fireEvent.click(screen.getByRole('button', { name: 'Aprobado' }))

  await waitFor(() => {
    expect(draftsApi.list).toHaveBeenCalledWith({ channel: 'LINKEDIN', status: 'APPROVED' })
  })
})

// ─── Test 8: "Aprobar" button is disabled while actionLoading === true ────────
// Validates: Requirements 4.5, 9.3
it('disables the "Aprobar" button while the approve request is in-flight', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])

  // approve never resolves — keeps actionLoading=true
  draftsApi.approve.mockReturnValue(new Promise(() => {}))

  render(<Drafts />)

  // Wait for the draft card to appear
  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  const approveBtn = screen.getByRole('button', { name: /aprobar/i })
  expect(approveBtn).not.toBeDisabled()

  // Click Aprobar
  fireEvent.click(approveBtn)

  // Button should now be disabled
  await waitFor(() => {
    expect(approveBtn).toBeDisabled()
  })
})

// ─── Test 9: "Rechazar" button is also disabled while actionLoading === true ──
// Validates: Requirements 4.6, 9.3
it('disables the "Rechazar" button while the reject request is in-flight', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])

  // reject never resolves — keeps actionLoading=true
  draftsApi.reject.mockReturnValue(new Promise(() => {}))

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  const rejectBtn = screen.getByRole('button', { name: /rechazar/i })
  expect(rejectBtn).not.toBeDisabled()

  fireEvent.click(rejectBtn)

  await waitFor(() => {
    expect(rejectBtn).toBeDisabled()
  })
})

// ─── Test 10: Both action buttons are disabled while actionLoading === true ───
// Validates: Requirements 9.3
it('disables both Aprobar and Rechazar buttons while an action is in-flight', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])
  draftsApi.approve.mockReturnValue(new Promise(() => {}))

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  const approveBtn = screen.getByRole('button', { name: /aprobar/i })
  const rejectBtn  = screen.getByRole('button', { name: /rechazar/i })

  // Both enabled before action
  expect(approveBtn).not.toBeDisabled()
  expect(rejectBtn).not.toBeDisabled()

  fireEvent.click(approveBtn)

  // Both disabled while in-flight (actionLoading is shared within DraftCard)
  await waitFor(() => {
    expect(approveBtn).toBeDisabled()
    expect(rejectBtn).toBeDisabled()
  })
})

// ─── Test 11: Buttons re-enable after approve resolves ───────────────────────
// Validates: Requirements 4.5, 9.3
it('re-enables action buttons after approve resolves', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])

  let resolveApprove
  draftsApi.approve.mockReturnValue(
    new Promise((res) => { resolveApprove = res })
  )

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  // Click the approve button (identified by its class since text changes to "…" while loading)
  const approveBtn = document.querySelector('[class*="approve"][class*="btn-sm"]')
  expect(approveBtn).not.toBeNull()
  fireEvent.click(approveBtn)

  // Button should be disabled while in-flight
  await waitFor(() => {
    const btn = document.querySelector('[class*="approve"][class*="btn-sm"]')
    expect(btn).toBeDisabled()
  })

  // Resolve the approve call
  await act(async () => { resolveApprove({ ...PENDING_DRAFT, status: 'APPROVED' }) })

  // After finally block runs, actionLoading=false → button re-enables
  // Re-query to avoid stale reference
  await waitFor(() => {
    const btn = document.querySelector('[class*="approve"][class*="btn-sm"]')
    expect(btn).not.toBeDisabled()
  })
})

// ─── Test 12: approve calls draftsApi.approve with the correct draft id ───────
// Validates: Requirements 4.5
it('calls draftsApi.approve with the correct draft id when Aprobar is clicked', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])
  draftsApi.approve.mockResolvedValue({ ...PENDING_DRAFT, status: 'APPROVED' })

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: /aprobar/i }))
  })

  await waitFor(() => {
    expect(draftsApi.approve).toHaveBeenCalledWith(PENDING_DRAFT.id)
  })
})

// ─── Test 13: reject calls draftsApi.reject with id and reason ───────────────
// Validates: Requirements 4.6
it('calls draftsApi.reject with the correct id and reason when Rechazar is clicked', async () => {
  draftsApi.list.mockResolvedValue([PENDING_DRAFT])
  draftsApi.reject.mockResolvedValue({ ...PENDING_DRAFT, status: 'REJECTED' })

  render(<Drafts />)

  await waitFor(() => {
    expect(screen.getByText(PENDING_DRAFT.summary)).toBeInTheDocument()
  })

  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: /rechazar/i }))
  })

  await waitFor(() => {
    expect(draftsApi.reject).toHaveBeenCalledWith(
      PENDING_DRAFT.id,
      'Rechazado desde el panel'
    )
  })
})

// ─── Test 14: draftsApi.list is called on mount with no filters ───────────────
// Validates: Requirements 4.1
it('calls draftsApi.list with no filters on initial mount', async () => {
  draftsApi.list.mockResolvedValue([])

  render(<Drafts />)

  await waitFor(() => {
    // The first call should have no channel or status params
    expect(draftsApi.list).toHaveBeenCalledWith({})
  })
})
