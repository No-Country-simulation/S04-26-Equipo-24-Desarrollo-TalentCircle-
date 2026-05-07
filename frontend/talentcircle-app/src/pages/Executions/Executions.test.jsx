/**
 * Component tests for the Executions page.
 * Task 9.1 — Requirements: 6.2, 6.4, 9.1, 9.3
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
import Executions from './Executions'

// ─── Mock adminApi ────────────────────────────────────────────────────────────
vi.mock('../../api/adminApi', () => ({
  default: {
    getExecutions: vi.fn(),
    triggerExecution: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
const mockShowToast = vi.fn()
const mockCurrentUser = { email: 'admin@test.com' }

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(),
}))

import adminApi from '../../api/adminApi'
import { useAppStore } from '../../store/useAppStore'

const SAMPLE_EXECUTIONS = [
  {
    id: 'exec-1',
    weekStart: '2025-01-06',
    weekEnd: '2025-01-12',
    status: 'COMPLETED',
    startedAt: '2025-01-06T08:00:00Z',
    completedAt: '2025-01-06T09:30:00Z',
  },
  {
    id: 'exec-2',
    weekStart: '2025-01-13',
    weekEnd: '2025-01-19',
    status: 'RUNNING',
    startedAt: '2025-01-13T08:00:00Z',
    completedAt: null,
  },
]

beforeEach(() => {
  vi.clearAllMocks()
  useAppStore.mockReturnValue({
    showToast: mockShowToast,
    currentUser: mockCurrentUser,
  })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ─── Test 1: Skeleton renders while loading === true ─────────────────────────
it('renders skeleton rows while loading', async () => {
  // Never resolves during this test — keeps loading=true
  adminApi.getExecutions.mockReturnValue(new Promise(() => {}))

  render(<Executions />)

  // Skeleton blocks should be present (5 rows × 6 cells = 30 skeleton blocks)
  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBeGreaterThan(0)

  // The trigger button should still be visible
  expect(screen.getByTestId('trigger-btn')).toBeInTheDocument()
})

// ─── Test 2: Table rows render after data loads ───────────────────────────────
it('renders execution rows after data loads', async () => {
  adminApi.getExecutions.mockResolvedValue(SAMPLE_EXECUTIONS)

  render(<Executions />)

  await waitFor(() => {
    expect(screen.getByText('#exec-1')).toBeInTheDocument()
    expect(screen.getByText('#exec-2')).toBeInTheDocument()
  })

  // Skeleton should be gone
  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBe(0)
})

// ─── Test 3: Trigger button is disabled while actionLoading === true ──────────
it('disables trigger button and shows spinner while request is in-flight', async () => {
  adminApi.getExecutions.mockResolvedValue([])

  // triggerExecution never resolves — keeps actionLoading=true
  let resolveTrigger
  adminApi.triggerExecution.mockReturnValue(new Promise((res) => { resolveTrigger = res }))

  render(<Executions />)

  // Wait for initial load to finish
  await waitFor(() => {
    expect(screen.queryByText('No hay ejecuciones registradas')).toBeInTheDocument()
  })

  const btn = screen.getByTestId('trigger-btn')
  expect(btn).not.toBeDisabled()

  // Click the trigger button
  fireEvent.click(btn)

  // Button should now be disabled and show spinner text
  await waitFor(() => {
    expect(btn).toBeDisabled()
    expect(screen.getByText(/Disparando/)).toBeInTheDocument()
  })

  // Resolve the trigger and verify button re-enables
  await act(async () => { resolveTrigger() })

  await waitFor(() => {
    expect(btn).not.toBeDisabled()
    expect(screen.getByText(/Disparar ejecución manual/)).toBeInTheDocument()
  })
})

// ─── Test 4: Success toast shown on HTTP 202 ─────────────────────────────────
it('shows success toast after trigger resolves (HTTP 202)', async () => {
  adminApi.getExecutions.mockResolvedValue([])
  adminApi.triggerExecution.mockResolvedValue(undefined)

  render(<Executions />)

  await waitFor(() => {
    expect(screen.queryByText('No hay ejecuciones registradas')).toBeInTheDocument()
  })

  fireEvent.click(screen.getByTestId('trigger-btn'))

  await waitFor(() => {
    expect(mockShowToast).toHaveBeenCalledWith(
      '🚀',
      'Ejecución iniciada',
      expect.stringContaining('exitosamente'),
    )
  })
})

// ─── Test 5: triggerExecution called with currentUser email ──────────────────
it('calls triggerExecution with the current user email', async () => {
  adminApi.getExecutions.mockResolvedValue([])
  adminApi.triggerExecution.mockResolvedValue(undefined)

  render(<Executions />)

  await waitFor(() => {
    expect(screen.queryByText('No hay ejecuciones registradas')).toBeInTheDocument()
  })

  fireEvent.click(screen.getByTestId('trigger-btn'))

  await waitFor(() => {
    expect(adminApi.triggerExecution).toHaveBeenCalledWith('admin@test.com')
  })
})

// ─── Test 6: Empty state when API returns [] ─────────────────────────────────
it('shows empty state when API returns empty array', async () => {
  adminApi.getExecutions.mockResolvedValue([])

  render(<Executions />)

  await waitFor(() => {
    expect(screen.getByText('No hay ejecuciones registradas')).toBeInTheDocument()
  })
})

// ─── Test 7: loading is false after fetch settles (success) ──────────────────
it('removes skeleton after successful fetch', async () => {
  adminApi.getExecutions.mockResolvedValue(SAMPLE_EXECUTIONS)

  render(<Executions />)

  await waitFor(() => {
    const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
    expect(skeletonBlocks.length).toBe(0)
  })
})

// ─── Test 8: loading is false after fetch settles (error) ────────────────────
it('removes skeleton after failed fetch', async () => {
  adminApi.getExecutions.mockRejectedValue(new Error('Network error'))

  render(<Executions />)

  await waitFor(() => {
    const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
    expect(skeletonBlocks.length).toBe(0)
  })
})
