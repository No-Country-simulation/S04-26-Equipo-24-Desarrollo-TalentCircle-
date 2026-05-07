import { useEffect } from 'react'
import AppRouter from './router/AppRouter'
import Toast from './components/Toast'
import { useAppStore } from './store/useAppStore'

export default function App() {
  useEffect(() => {
    useAppStore.getState().initAuth()
  }, [])

  return (
    <>
      <AppRouter />
      <Toast />
    </>
  )
}
