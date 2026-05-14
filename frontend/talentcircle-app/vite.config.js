import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  // ── Dev proxy ────────────────────────────────────────────────────────────
  // En desarrollo local, Vite redirige /api/* y /discord/* al backend en :8081
  // En producción (Docker), nginx hace lo mismo — ver nginx/default.conf
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/discord': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },

  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.js'],
    globals: true,
  },
})
