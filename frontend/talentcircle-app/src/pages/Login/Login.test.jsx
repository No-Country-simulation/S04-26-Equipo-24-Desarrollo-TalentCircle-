/**
 * Component tests for the Login page.
 * Task 7.1 — Requirements: 2.1, 2.3, 2.6
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
import Login from './Login'

// ─── Mock react-router-dom ────────────────────────────────────────────────────
const mockNavigate = vi.fn()

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

// ─── Mock authApi ─────────────────────────────────────────────────────────────
vi.mock('../../api/authApi', () => ({
  default: {
    login: vi.fn(),
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Login.jsx uses the destructuring pattern: const { login } = useAppStore()
const mockLogin = vi.fn()

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn(() => ({ login: mockLogin })),
}))

import authApi from '../../api/authApi'
import { useAppStore } from '../../store/useAppStore'

// ─── Sample data ──────────────────────────────────────────────────────────────
const SAMPLE_LOGIN_RESPONSE = {
  accessToken: 'access-token-abc',
  refreshToken: 'refresh-token-xyz',
  expiresIn: '8h',
  user: { id: 'u1', email: 'editor@talentcircle.com', fullName: 'Editor User', role: 'EDITOR', active: true },
}

beforeEach(() => {
  vi.clearAllMocks()
  // Re-apply store mock after clearAllMocks
  useAppStore.mockReturnValue({ login: mockLogin })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ─── Helper: fill and submit the login form ───────────────────────────────────
function fillAndSubmit(email = 'editor@talentcircle.com', password = 'secret123') {
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: email },
  })
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: password },
  })
  fireEvent.click(screen.getByRole('button', { name: /ingresar/i }))
}

// ─── Test 1: Loading spinner appears and submit button is disabled while in-flight ─
it('shows loading spinner and disables submit button while request is in-flight', async () => {
  // authApi.login never resolves — keeps loading=true
  authApi.login.mockReturnValue(new Promise(() => {}))

  render(<Login />)

  // Before submit: button is enabled and shows normal text
  const btn = screen.getByRole('button', { name: /ingresar/i })
  expect(btn).not.toBeDisabled()

  // Submit the form
  fillAndSubmit()

  // Button should now be disabled
  await waitFor(() => {
    expect(btn).toBeDisabled()
  })

  // Spinner element should be visible (rendered as <span className={styles.spinner} />)
  // The button no longer contains the text "Ingresar" — it contains the spinner span
  expect(btn).not.toHaveTextContent('Ingresar al panel →')
  const spinner = btn.querySelector('span')
  expect(spinner).toBeInTheDocument()
})

// ─── Test 2: navigate('/dashboard') is called on successful login response ────
it('calls navigate("/dashboard") after a successful login', async () => {
  authApi.login.mockResolvedValue(SAMPLE_LOGIN_RESPONSE)

  render(<Login />)

  fillAndSubmit()

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  // Store login action should also have been called with the response
  expect(mockLogin).toHaveBeenCalledWith(SAMPLE_LOGIN_RESPONSE)
})

// ─── Test 3: Button re-enables after error (toast handled by interceptor) ─────
it('re-enables the submit button after authApi.login rejects', async () => {
  const authError = new Error('Request failed with status code 401')
  authError.response = { status: 401, data: { message: 'Credenciales inválidas' } }
  authApi.login.mockRejectedValue(authError)

  render(<Login />)

  const btn = screen.getByRole('button', { name: /ingresar/i })

  fillAndSubmit()

  // Button should re-enable after the error settles
  await waitFor(() => {
    expect(btn).not.toBeDisabled()
  })

  // Normal button text should be restored
  expect(btn).toHaveTextContent('Ingresar al panel →')

  // navigate should NOT have been called
  expect(mockNavigate).not.toHaveBeenCalled()
})
