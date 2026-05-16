import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  // Carga las variables de entorno (.env)
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          // Usa la variable del .env o localhost por defecto
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        }
      }
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['src/test/setup.js'],
      globals: true,
    },
  }
})