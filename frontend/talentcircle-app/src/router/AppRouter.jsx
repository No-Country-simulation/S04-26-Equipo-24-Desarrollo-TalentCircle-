import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAppStore } from "../store/useAppStore";

import Layout from "../components/Layout";

import Login from "../pages/Login/Login";
import Dashboard from "../pages/Dashboard/Dashboard";
import Drafts from "../pages/Drafts/Drafts";
import Executions from "../pages/Executions/Executions";
import Admin from "../pages/Admin/Admin";

// ─────────────────────────────────────────────────────────────
// Protected Route → requiere autenticación
// ─────────────────────────────────────────────────────────────
function ProtectedRoute({ children }) {
  const isAuthenticated = useAppStore((s) => s.isAuthenticated);

  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

// ─────────────────────────────────────────────────────────────
// Public Route → evita volver al login estando autenticado
// ─────────────────────────────────────────────────────────────
function PublicRoute({ children }) {
  const isAuthenticated = useAppStore((s) => s.isAuthenticated);

  return !isAuthenticated ? children : <Navigate to="/dashboard" replace />;
}

// ─────────────────────────────────────────────────────────────
// Role Route → protección por roles
// ─────────────────────────────────────────────────────────────
function RoleRoute({ children, allowedRoles }) {
  const user = useAppStore((s) => s.user);

  // Si no existe usuario → login
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Si el rol no está permitido → dashboard
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ───────────────── LOGIN ───────────────── */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        {/* ─────────────── APP PRIVADA ─────────────── */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          {/* Redirect raíz */}
          <Route index element={<Navigate to="/dashboard" replace />} />

          {/* Rutas privadas */}
          <Route path="dashboard" element={<Dashboard />} />

          <Route path="drafts" element={<Drafts />} />

          <Route path="executions" element={<Executions />} />

          {/* Ruta ADMIN */}
          <Route
            path="admin"
            element={
              <RoleRoute allowedRoles={["ADMIN"]}>
                <Admin />
              </RoleRoute>
            }
          />
        </Route>

        {/* ─────────────── FALLBACK ─────────────── */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
