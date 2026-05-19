import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAppStore } from '../store/useAppStore'
import Layout from '../components/Layout'
import Login from '../pages/Login/Login'
import Dashboard from '../pages/Dashboard/Dashboard'
import Drafts from '../pages/Drafts/Drafts'
import Executions from '../pages/Executions/Executions'
import Admin from '../pages/Admin/Admin'

const LandingPage = lazy(() => import('../pages/LandingPage/LandingPage'))

function ProtectedRoute({ children }) {
  const isAuthenticated = useAppStore((s) => s.isAuthenticated)
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={
          <Suspense fallback={<div style={{display:'flex',justifyContent:'center',alignItems:'center',height:'100vh'}}>Loading...</div>}>
            <LandingPage />
          </Suspense>
        } />
        <Route path="/login" element={<Login />} />
        <Route path="/app" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/app/dashboard" replace />} />
          <Route path="dashboard"  element={<Dashboard />} />
          <Route path="drafts"     element={<Drafts />} />
          <Route path="executions" element={<Executions />} />
          <Route path="admin"      element={<Admin />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
