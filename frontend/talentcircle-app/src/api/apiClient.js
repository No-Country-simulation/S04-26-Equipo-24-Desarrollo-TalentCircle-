import axios from 'axios'
import { useAppStore } from '../store/useAppStore'

const BASE_URL = 'http://localhost:8081'

const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Instancia limpia SIN interceptores — usada exclusivamente para el refresh
// Evita que el 401 del refresh vuelva a entrar al interceptor (loop infinito)
export const refreshClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ─── Request interceptor ──────────────────────────────────────────────────────
apiClient.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken')
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ─── Response interceptor ─────────────────────────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,

  async (error) => {
    const { showToast } = useAppStore.getState()
    const originalRequest = error.config

    // ── Network error ─────────────────────────────────────────────────────────
    if (!error.response) {
      showToast('❌', 'Sin conexión', 'Sin conexión con el servidor. Verifica que el backend esté activo.')
      return Promise.reject(error)
    }

    const { status } = error.response

    // ── HTTP 401 ──────────────────────────────────────────────────────────────
    if (status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')

      // Sin refresh token o ya se intentó → limpiar sesión y redirigir
      if (!refreshToken || originalRequest._retry) {
        localStorage.clear()
        useAppStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      originalRequest._retry = true

      try {
        // Usar refreshClient (sin interceptores) para evitar el loop infinito
        const { data } = await refreshClient.post('/api/v1/auth/refresh', { refreshToken })

        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)

        originalRequest.headers['Authorization'] = `Bearer ${data.accessToken}`
        return apiClient(originalRequest)
      } catch {
        // El refresh devolvió error → limpiar sesión y redirigir
        localStorage.clear()
        useAppStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }

    // ── HTTP 403 ──────────────────────────────────────────────────────────────
    if (status === 403) {
      showToast('🚫', 'Sin permisos', 'No tienes permisos para realizar esta acción.')
      return Promise.reject(error)
    }

    // ── Todos los demás 4xx / 5xx ─────────────────────────────────────────────
    const message =
      error.response?.data?.message || 'Error de conexión. Intenta de nuevo.'
    showToast('❌', 'Error', message)
    return Promise.reject(error)
  }
)

export default apiClient
