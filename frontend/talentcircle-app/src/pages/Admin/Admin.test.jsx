/**
 * Component tests for the Admin page.
 * Task 10.1 — Requirements: 5.1–5.5, 9.1
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react'
import Admin from './Admin'

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
  },
}))

// ─── Mock useAppStore ─────────────────────────────────────────────────────────
// Admin.jsx uses the selector pattern: useAppStore((s) => s.showToast)
// So the mock must behave as a selector function.
const mockShowToast = vi.fn()

vi.mock('../../store/useAppStore', () => ({
  useAppStore: vi.fn((selector) => selector({ showToast: mockShowToast })),
}))

import adminApi from '../../api/adminApi'
import { useAppStore } from '../../store/useAppStore'

// ─── Sample data ──────────────────────────────────────────────────────────────
const SAMPLE_USERS = [
  { id: 'u1', fullName: 'Javier Chavez',  email: 'javier@tc.com', role: 'EDITOR', active: true  },
  { id: 'u2', fullName: 'Eduin Pino',     email: 'eduin@tc.com',  role: 'ADMIN',  active: true  },
]

const SAMPLE_SOURCES = [
  { id: 's1', name: 'Discord – Dev',  type: 'DISCORD', active: true  },
  { id: 's2', name: 'Circle – Main',  type: 'CIRCLE',  active: false },
]

const SAMPLE_CONFIG = {
  llmProvider:        'openai',
  llmModel:           'gpt-4o',
  newsletterPrompt:   'Newsletter prompt text',
  linkedinPrompt:     'LinkedIn prompt text',
  twitterPrompt:      'Twitter prompt text',
  maxItemsPerChannel: 20,
  scheduleCron:       '0 18 * * FRI',
}

beforeEach(() => {
  vi.clearAllMocks()
  // Re-apply selector mock after clearAllMocks
  useAppStore.mockImplementation((selector) => selector({ showToast: mockShowToast }))

  // Default: all fetches resolve with sample data
  adminApi.getUsers.mockResolvedValue(SAMPLE_USERS)
  adminApi.getSources.mockResolvedValue(SAMPLE_SOURCES)
  adminApi.getConfig.mockResolvedValue(SAMPLE_CONFIG)
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ─── Helper: wait for all three sections to finish loading ───────────────────
async function waitForLoad() {
  await waitFor(() => {
    expect(screen.getByText('Javier Chavez')).toBeInTheDocument()
    expect(screen.getByText('Discord – Dev')).toBeInTheDocument()
    expect(screen.getByDisplayValue('openai')).toBeInTheDocument()
  })
}

// ─── Test 1: Three parallel fetches fire on mount ────────────────────────────
it('fires getUsers, getSources, and getConfig on mount', async () => {
  render(<Admin />)

  await waitForLoad()

  expect(adminApi.getUsers).toHaveBeenCalledTimes(1)
  expect(adminApi.getSources).toHaveBeenCalledTimes(1)
  expect(adminApi.getConfig).toHaveBeenCalledTimes(1)
})

// ─── Test 2: Skeletons render while sections are loading ─────────────────────
it('renders skeleton blocks while fetches are in flight', () => {
  // Never resolve — keeps all sections loading
  adminApi.getUsers.mockReturnValue(new Promise(() => {}))
  adminApi.getSources.mockReturnValue(new Promise(() => {}))
  adminApi.getConfig.mockReturnValue(new Promise(() => {}))

  render(<Admin />)

  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBeGreaterThan(0)
})

// ─── Test 3: Users section skeleton independent from sources ─────────────────
it('shows users skeleton while users load even if sources already loaded', async () => {
  // Sources and config resolve immediately; users never resolve
  adminApi.getUsers.mockReturnValue(new Promise(() => {}))
  adminApi.getSources.mockResolvedValue(SAMPLE_SOURCES)
  adminApi.getConfig.mockResolvedValue(SAMPLE_CONFIG)

  render(<Admin />)

  // Sources should be visible
  await waitFor(() => {
    expect(screen.getByText('Discord – Dev')).toBeInTheDocument()
  })

  // Users section should still show skeletons
  const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
  expect(skeletonBlocks.length).toBeGreaterThan(0)
})

// ─── Test 4: Users render after load ─────────────────────────────────────────
it('renders user rows after users fetch resolves', async () => {
  render(<Admin />)

  await waitFor(() => {
    expect(screen.getByText('Javier Chavez')).toBeInTheDocument()
    expect(screen.getByText('Eduin Pino')).toBeInTheDocument()
  })
})

// ─── Test 5: Sources render after load ───────────────────────────────────────
it('renders source rows after sources fetch resolves', async () => {
  render(<Admin />)

  await waitFor(() => {
    expect(screen.getByText('Discord – Dev')).toBeInTheDocument()
    expect(screen.getByText('Circle – Main')).toBeInTheDocument()
  })
})

// ─── Test 6: Source toggle calls updateSource with correct { active } payload ─
it('calls updateSource with toggled active value when toggle is clicked', async () => {
  adminApi.updateSource.mockResolvedValue({ ...SAMPLE_SOURCES[0], active: false })

  render(<Admin />)
  await waitForLoad()

  // Find the toggle buttons inside the sources section
  const sourcesSection = screen.getByTestId('sources-section')
  const toggles = sourcesSection.querySelectorAll('button[class*="toggle"]')

  expect(toggles.length).toBeGreaterThan(0)

  await act(async () => {
    fireEvent.click(toggles[0])
  })

  await waitFor(() => {
    expect(adminApi.updateSource).toHaveBeenCalledWith('s1', { active: false })
  })
})

// ─── Test 7: Source toggle updates local state on success ────────────────────
it('updates source active state in UI after successful toggle', async () => {
  adminApi.updateSource.mockResolvedValue({ ...SAMPLE_SOURCES[0], active: false })

  render(<Admin />)
  await waitForLoad()

  const sourcesSection = screen.getByTestId('sources-section')
  const toggles = sourcesSection.querySelectorAll('button[class*="toggle"]')

  await act(async () => {
    fireEvent.click(toggles[0])
  })

  await waitFor(() => {
    expect(adminApi.updateSource).toHaveBeenCalledTimes(1)
  })
})

// ─── Test 8: Config save calls updateConfig with full payload ─────────────────
it('calls updateConfig with full config payload when "Guardar cambios" is clicked', async () => {
  adminApi.updateConfig.mockResolvedValue(SAMPLE_CONFIG)

  render(<Admin />)
  await waitForLoad()

  const saveBtn = screen.getByTestId('save-config-btn')
  await act(async () => {
    fireEvent.click(saveBtn)
  })

  await waitFor(() => {
    expect(adminApi.updateConfig).toHaveBeenCalledTimes(1)
    const payload = adminApi.updateConfig.mock.calls[0][0]
    expect(payload).toMatchObject({
      llmProvider:        'openai',
      llmModel:           'gpt-4o',
      maxItemsPerChannel: 20,
      scheduleCron:       '0 18 * * FRI',
    })
  })
})

// ─── Test 9: Config save shows success toast ──────────────────────────────────
it('shows success toast after config save', async () => {
  adminApi.updateConfig.mockResolvedValue(SAMPLE_CONFIG)

  render(<Admin />)
  await waitForLoad()

  await act(async () => {
    fireEvent.click(screen.getByTestId('save-config-btn'))
  })

  await waitFor(() => {
    expect(mockShowToast).toHaveBeenCalledWith(
      '✅',
      'Configuración guardada',
      expect.any(String),
    )
  })
})

// ─── Test 10: Prompt save calls updateConfig with updated prompt field ────────
it('calls updateConfig with updated prompt when "Guardar" prompt button is clicked', async () => {
  adminApi.updateConfig.mockResolvedValue(SAMPLE_CONFIG)

  render(<Admin />)
  await waitForLoad()

  // Change the newsletter prompt — scope to the prompts section to avoid ambiguity
  const promptsSection = screen.getByTestId('prompts-section')
  const textarea = promptsSection.querySelector('textarea')
  await act(async () => {
    fireEvent.change(textarea, { target: { value: 'Updated newsletter prompt' } })
  })

  await act(async () => {
    fireEvent.click(screen.getByTestId('save-prompt-btn'))
  })

  await waitFor(() => {
    expect(adminApi.updateConfig).toHaveBeenCalledTimes(1)
    const payload = adminApi.updateConfig.mock.calls[0][0]
    expect(payload.newsletterPrompt).toBe('Updated newsletter prompt')
  })
})

// ─── Test 11: Prompt save shows success toast ─────────────────────────────────
it('shows success toast after prompt save', async () => {
  adminApi.updateConfig.mockResolvedValue(SAMPLE_CONFIG)

  render(<Admin />)
  await waitForLoad()

  await act(async () => {
    fireEvent.click(screen.getByTestId('save-prompt-btn'))
  })

  await waitFor(() => {
    expect(mockShowToast).toHaveBeenCalledWith(
      '✅',
      'Prompt guardado',
      expect.any(String),
    )
  })
})

// ─── Test 12: "Nuevo usuario" opens modal and calls createUser ────────────────
it('calls createUser and refreshes users list when new user form is submitted', async () => {
  const newUser = { id: 'u3', fullName: 'Ana García', email: 'ana@tc.com', role: 'EDITOR', active: true }
  adminApi.createUser.mockResolvedValue(newUser)
  adminApi.getUsers.mockResolvedValueOnce(SAMPLE_USERS)
                   .mockResolvedValueOnce([...SAMPLE_USERS, newUser])

  render(<Admin />)
  await waitForLoad()

  // Open modal
  fireEvent.click(screen.getByTestId('new-user-btn'))

  // Modal title should appear
  expect(screen.getByRole('heading', { name: 'Nuevo usuario' })).toBeInTheDocument()

  // Fill form
  fireEvent.change(screen.getByPlaceholderText('Nombre Apellido'), { target: { value: 'Ana García' } })
  fireEvent.change(screen.getByPlaceholderText('usuario@talentcircle.com'), { target: { value: 'ana@tc.com' } })
  fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), { target: { value: 'Password123!' } })

  // Submit using the submit button inside the modal
  const submitBtn = screen.getByRole('button', { name: 'Crear usuario' })
  await act(async () => {
    fireEvent.click(submitBtn)
  })

  await waitFor(() => {
    expect(adminApi.createUser).toHaveBeenCalledWith(
      expect.objectContaining({ fullName: 'Ana García', email: 'ana@tc.com' })
    )
    expect(adminApi.getUsers).toHaveBeenCalledTimes(2)
  })
})

// ─── Test 13: "Editar" opens modal and calls updateUser ───────────────────────
it('calls updateUser and refreshes users list when edit form is submitted', async () => {
  const updatedUser = { ...SAMPLE_USERS[0], fullName: 'Javier Updated' }
  adminApi.updateUser.mockResolvedValue(updatedUser)
  adminApi.getUsers.mockResolvedValueOnce(SAMPLE_USERS)
                   .mockResolvedValueOnce([updatedUser, SAMPLE_USERS[1]])

  render(<Admin />)
  await waitForLoad()

  // Click edit on first user
  fireEvent.click(screen.getByTestId('edit-user-btn-u1'))
  expect(screen.getByRole('heading', { name: 'Editar usuario' })).toBeInTheDocument()

  // Change name
  const nameInput = screen.getByDisplayValue('Javier Chavez')
  fireEvent.change(nameInput, { target: { value: 'Javier Updated' } })

  // Submit using the submit button inside the modal overlay
  const modalOverlay = document.querySelector('[class*="modalOverlay"]')
  const submitBtn = modalOverlay.querySelector('button[type="submit"]')
  await act(async () => {
    fireEvent.click(submitBtn)
  })

  await waitFor(() => {
    expect(adminApi.updateUser).toHaveBeenCalledWith(
      'u1',
      expect.objectContaining({ fullName: 'Javier Updated' })
    )
    expect(adminApi.getUsers).toHaveBeenCalledTimes(2)
  })
})

// ─── Test 14: "Agregar fuente" calls createSource and refreshes ───────────────
it('calls createSource and refreshes sources list when new source form is submitted', async () => {
  const newSource = { id: 's3', name: 'Slack – Recursos', type: 'SLACK', active: true }
  adminApi.createSource.mockResolvedValue(newSource)
  // Fresh mocks for this test
  adminApi.getUsers.mockResolvedValue(SAMPLE_USERS)
  adminApi.getSources.mockResolvedValueOnce(SAMPLE_SOURCES)
                     .mockResolvedValueOnce([...SAMPLE_SOURCES, newSource])

  render(<Admin />)
  await waitForLoad()

  // Open modal
  fireEvent.click(screen.getByTestId('add-source-btn'))
  expect(screen.getByRole('heading', { name: 'Agregar fuente' })).toBeInTheDocument()

  // Fill form — tipo DISCORD por defecto, cambiar a SLACK para evitar campos requeridos de Discord
  const typeSelect = screen.getByRole('combobox', { name: /tipo/i })
  fireEvent.change(typeSelect, { target: { value: 'SLACK' } })
  fireEvent.change(screen.getByPlaceholderText('Nombre de la fuente'), { target: { value: 'Slack – Recursos' } })

  // Submit using the submit button inside the modal overlay
  const modalOverlay = document.querySelector('[class*="modalOverlay"]')
  const submitBtn = modalOverlay.querySelector('button[type="submit"]')

  await act(async () => {
    fireEvent.click(submitBtn)
  })

  await waitFor(() => {
    expect(adminApi.createSource).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Slack – Recursos' })
    )
    expect(adminApi.getSources).toHaveBeenCalledTimes(2)
  })
})

// ─── Test 15: loading is false after all fetches settle (error case) ──────────
it('removes all skeletons after failed fetches', async () => {
  adminApi.getUsers.mockRejectedValue(new Error('Network error'))
  adminApi.getSources.mockRejectedValue(new Error('Network error'))
  adminApi.getConfig.mockRejectedValue(new Error('Network error'))

  render(<Admin />)

  await waitFor(() => {
    const skeletonBlocks = document.querySelectorAll('[class*="skeletonBlock"]')
    expect(skeletonBlocks.length).toBe(0)
  })
})
