import { create } from 'zustand'

// Inicialización síncrona — se ejecuta una vez al importar el módulo,
// antes del primer render. Evita el flash de "no autenticado" y el reinicio.
function getInitialAuthState() {
  const accessToken = localStorage.getItem('accessToken')
  const refreshToken = localStorage.getItem('refreshToken')
  const userRaw = localStorage.getItem('currentUser')
  if (accessToken && refreshToken) {
    return {
      isAuthenticated: true,
      accessToken,
      refreshToken,
      currentUser: userRaw ? JSON.parse(userRaw) : null,
    }
  }
  return { isAuthenticated: false, accessToken: null, refreshToken: null, currentUser: null }
}

export const useAppStore = create((set) => ({
  // Auth — estado inicial leído sincrónicamente desde localStorage
  ...getInitialAuthState(),

  // Auth actions
  login: (loginResponse) => {
    const { accessToken, refreshToken, expiresIn, user } = loginResponse
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('currentUser', JSON.stringify(user))
    set({ isAuthenticated: true, currentUser: user, accessToken, refreshToken, expiresIn })
  },
  logout: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('currentUser')
    set({ isAuthenticated: false, currentUser: null, accessToken: null, refreshToken: null })
  },
  // initAuth se mantiene por compatibilidad pero ya no es necesario llamarlo
  initAuth: () => {
    const accessToken = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')
    const userRaw = localStorage.getItem('currentUser')
    if (accessToken && refreshToken) {
      const currentUser = userRaw ? JSON.parse(userRaw) : null
      set({ isAuthenticated: true, accessToken, refreshToken, currentUser })
    }
  },

  // UI state
  toast: null,
  showToast: (icon, title, body) => {
    set({ toast: { icon, title, body, id: Date.now() } })
    setTimeout(() => set({ toast: null }), 3800)
  },
  modalDraftId: null,
  openModal: (id) => set({ modalDraftId: id }),
  closeModal: () => set({ modalDraftId: null }),

  // ── Pipeline state ──────────────────────────────────────────────────────────
  // 'idle' | 'running' | 'completed' | 'failed'
  pipelineStatus: 'idle',
  pipelineRunning: false,
  pipelineError: null,        // mensaje de error si status === 'failed'
  lastExecutionId: null,      // ID de la última ejecución disparada
  pipelineAlertDismissed: false, // true cuando el usuario cierra la alerta

  setPipelineRunning: (running) => set({ pipelineRunning: running }),
  setPipelineStatus: (status, error = null, executionId = null) =>
    set({
      pipelineStatus: status,
      pipelineError: error,
      lastExecutionId: executionId ?? null,
      pipelineAlertDismissed: false,
    }),
  dismissPipelineAlert: () => set({ pipelineAlertDismissed: true }),

  // ── Draft counts — actualizados por Dashboard al cargar ───────────────────
  draftTotalCount: null,    // null = aún no cargado
  draftPendingCount: null,
  setDraftCounts: (total, pending) => set({ draftTotalCount: total, draftPendingCount: pending }),

  // Data (no initial mock values — pages own their local state)
  drafts: [],
  executions: [],
  feedItems: [],

  // Setters
  setDrafts: (drafts) => set({ drafts }),
  setExecutions: (executions) => set({ executions }),
  setFeedItems: (feedItems) => set({ feedItems }),

  // Draft updaters
  updateDraftContent: (id, content) =>
    set((s) => ({ drafts: s.drafts.map((d) => d.id === id ? { ...d, content } : d) })),
  updateDraftStatus: (id, status, extra = {}) =>
    set((s) => ({ drafts: s.drafts.map((d) => d.id === id ? { ...d, status, ...extra } : d) })),
}))
