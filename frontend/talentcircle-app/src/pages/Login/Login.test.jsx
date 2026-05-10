/**
 * Component tests for the Login page.
 * Task 7.1 — Requirements: 2.1, 2.3, 2.6
 * Tasks 5.1–5.5 — Requirements: 1.1, 1.4, 2.1, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react'
import * as fc from 'fast-check'
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
  useAppStore.mockReturnValue({ login: mockLogin })
})

afterEach(() => {
  vi.restoreAllMocks()
  cleanup()
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

// ─── Helper: render into a dedicated container and return cleanup fn ──────────
function renderInContainer() {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const result = render(<Login />, { container })
  return {
    container,
    ...result,
    dispose: () => {
      result.unmount()
      document.body.removeChild(container)
    },
  }
}

// ─── Test 1 (fixed): Loading spinner appears and submit button is disabled while in-flight ─
// Requirements: 1.1, 3.1, 3.2
it('shows loading spinner and disables submit button while request is in-flight', async () => {
  // authApi.login never resolves — keeps loading=true
  authApi.login.mockReturnValue(new Promise(() => {}))

  render(<Login />)

  // Fill both fields first so the button becomes enabled
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: 'editor@talentcircle.com' },
  })
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: 'secret123' },
  })

  // Before submit: button is enabled (fields are filled)
  const btn = screen.getByRole('button', { name: /ingresar/i })
  expect(btn).not.toBeDisabled()

  // Submit the form
  fireEvent.click(btn)

  // Button should now be disabled (loading=true)
  await waitFor(() => {
    expect(btn).toBeDisabled()
  })

  // Spinner element should be visible
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

  await waitFor(() => {
    expect(btn).not.toBeDisabled()
  })

  expect(btn).toHaveTextContent('Ingresar al panel →')
  expect(mockNavigate).not.toHaveBeenCalled()
})

// ─── Property-based tests ─────────────────────────────────────────────────────

// Feature: login-button-validation, Property 1: Campos vacíos o solo-espacios deshabilitan el botón
// Validates: Requirements 1.1, 2.1, 4.1, 4.2
it('Property 1: empty or whitespace-only fields disable the button and apply disabled style', () => {
  // At least one field has trim().length === 0 (empty or whitespace-only)
  const emptyOrWhitespace = fc.string().filter((s) => s.trim().length === 0)
  const anyString = fc.string()

  // Generate pairs where at least one field is empty/whitespace
  const arbInvalidPair = fc.oneof(
    // email is empty/whitespace, password is anything
    fc.tuple(emptyOrWhitespace, anyString),
    // email is anything, password is empty/whitespace
    fc.tuple(anyString, emptyOrWhitespace),
  )

  authApi.login.mockReturnValue(new Promise(() => {}))

  fc.assert(
    fc.property(arbInvalidPair, ([email, password]) => {
      const { container, dispose } = renderInContainer()

      fireEvent.change(container.querySelector('input[placeholder="editor@talentcircle.com"]'), {
        target: { value: email },
      })
      fireEvent.change(container.querySelector('input[placeholder="••••••••"]'), {
        target: { value: password },
      })

      const btn = container.querySelector('button[type="submit"]')
      const isDisabled = btn.disabled === true
      const hasDisabledClass = btn.className.includes('btnLoginDisabled')

      dispose()

      return isDisabled && hasDisabledClass
    }),
    { numRuns: 100 },
  )
})

// Feature: login-button-validation, Property 2: Campos llenos habilitan el botón
// Validates: Requirements 1.4, 2.3
it('Property 2: filled fields enable the button with active style', () => {
  // Both fields must have trim().length > 0
  const nonEmptyString = fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0)

  authApi.login.mockReturnValue(new Promise(() => {}))

  fc.assert(
    fc.property(nonEmptyString, nonEmptyString, (email, password) => {
      const { container, dispose } = renderInContainer()

      fireEvent.change(container.querySelector('input[placeholder="editor@talentcircle.com"]'), {
        target: { value: email },
      })
      fireEvent.change(container.querySelector('input[placeholder="••••••••"]'), {
        target: { value: password },
      })

      const btn = container.querySelector('button[type="submit"]')
      const isEnabled = btn.disabled === false
      const hasNoDisabledClass = !btn.className.includes('btnLoginDisabled')

      dispose()

      return isEnabled && hasNoDisabledClass
    }),
    { numRuns: 100 },
  )
})

// Feature: login-button-validation, Property 3: Loading deshabilita el botón
// Validates: Requirements 3.1
it('Property 3: loading state disables the button regardless of field content', async () => {
  // Use only non-empty/non-whitespace strings so the form is always valid and
  // we can actually trigger loading=true by submitting. This covers the property:
  // "when loading=true, button is disabled regardless of field content".
  const nonEmptyString = fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0)

  await fc.assert(
    fc.asyncProperty(nonEmptyString, nonEmptyString, async (email, password) => {
      // Mock login to never resolve so loading stays true
      authApi.login.mockReturnValue(new Promise(() => {}))

      const { container, dispose } = renderInContainer()

      // Fill fields with the generated values
      fireEvent.change(container.querySelector('input[placeholder="editor@talentcircle.com"]'), {
        target: { value: email },
      })
      fireEvent.change(container.querySelector('input[placeholder="••••••••"]'), {
        target: { value: password },
      })

      const btn = container.querySelector('button[type="submit"]')
      const form = container.querySelector('form')

      // Submit the form — triggers handleSubmit → setLoading(true)
      fireEvent.submit(form)

      // setLoading(true) is synchronous, so button is disabled immediately after submit
      await waitFor(
        () => {
          expect(btn).toBeDisabled()
        },
        { container },
      )

      const isDisabled = btn.disabled === true

      dispose()

      return isDisabled
    }),
    { numRuns: 100 },
  )
}, 30000)

// ─── Example-based tests ──────────────────────────────────────────────────────

// Test A — Transition empty → filled: typing in both fields enables the button
// Requirements: 2.4
it('Test A: typing in both fields transitions button from disabled to enabled', () => {
  authApi.login.mockReturnValue(new Promise(() => {}))

  render(<Login />)

  const btn = screen.getByRole('button', { name: /ingresar/i })

  // Initially disabled (both fields empty)
  expect(btn).toBeDisabled()
  expect(btn.className).toContain('btnLoginDisabled')

  // Fill email field only — still disabled
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: 'editor@talentcircle.com' },
  })
  expect(btn).toBeDisabled()

  // Fill password field — now enabled
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: 'secret123' },
  })
  expect(btn).not.toBeDisabled()
  expect(btn.className).not.toContain('btnLoginDisabled')
})

// Test B — Transition filled → empty: clearing a field disables the button
// Requirements: 2.5
it('Test B: clearing a field transitions button from enabled to disabled', () => {
  authApi.login.mockReturnValue(new Promise(() => {}))

  render(<Login />)

  const btn = screen.getByRole('button', { name: /ingresar/i })

  // Fill both fields — button enabled
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: 'editor@talentcircle.com' },
  })
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: 'secret123' },
  })
  expect(btn).not.toBeDisabled()
  expect(btn.className).not.toContain('btnLoginDisabled')

  // Clear the password field — button disabled again
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: '' },
  })
  expect(btn).toBeDisabled()
  expect(btn.className).toContain('btnLoginDisabled')
})

// Test C — Spinner visible during loading: with filled fields, submitting shows spinner and disables button
// Requirements: 3.2
it('Test C: submitting with filled fields shows spinner and disables button', async () => {
  authApi.login.mockReturnValue(new Promise(() => {}))

  render(<Login />)

  // Fill both fields
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: 'editor@talentcircle.com' },
  })
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: 'secret123' },
  })

  const btn = screen.getByRole('button', { name: /ingresar/i })
  expect(btn).not.toBeDisabled()

  // Submit
  fireEvent.click(btn)

  await waitFor(() => {
    expect(btn).toBeDisabled()
  })

  // Spinner is visible, text is hidden
  const spinner = btn.querySelector('span')
  expect(spinner).toBeInTheDocument()
  expect(btn).not.toHaveTextContent('Ingresar al panel →')
})

// Test D — Post-error recovery: after authApi.login rejects and loading returns to false,
// button re-enables if fields are still filled
// Requirements: 3.3, 3.4
it('Test D: button re-enables after error if fields are still filled', async () => {
  const authError = new Error('Request failed with status code 401')
  authError.response = { status: 401, data: { message: 'Credenciales inválidas' } }
  authApi.login.mockRejectedValue(authError)

  render(<Login />)

  // Fill both fields
  fireEvent.change(screen.getByPlaceholderText('editor@talentcircle.com'), {
    target: { value: 'editor@talentcircle.com' },
  })
  fireEvent.change(screen.getByPlaceholderText('••••••••'), {
    target: { value: 'secret123' },
  })

  const btn = screen.getByRole('button', { name: /ingresar/i })
  expect(btn).not.toBeDisabled()

  // Submit — triggers error
  fireEvent.click(btn)

  // After error settles, loading=false and fields still filled → button re-enables
  await waitFor(() => {
    expect(btn).not.toBeDisabled()
  })

  expect(btn).toHaveTextContent('Ingresar al panel →')
  expect(btn.className).not.toContain('btnLoginDisabled')
  expect(mockNavigate).not.toHaveBeenCalled()
})
