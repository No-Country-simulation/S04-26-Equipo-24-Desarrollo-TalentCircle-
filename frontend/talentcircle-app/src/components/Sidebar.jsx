import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, FileText, Settings, Zap, LogOut, Layers, Link } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import styles from './Sidebar.module.css'

const NAV = [
  { to: '/dashboard',  icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/drafts',     icon: FileText,         label: 'Borradores' },
  { to: '/executions', icon: Zap,              label: 'Ejecuciones' },
]
const ADMIN_NAV = [
  { to: '/admin', icon: Settings, label: 'Administración' },
  { to: '#',      icon: Link,     label: 'Integraciones' },
  { to: '#',      icon: Layers,   label: 'Plantillas IA' },
]

/** Genera las iniciales a partir del fullName: "Ana López" → "AL" */
function getInitials(fullName) {
  if (!fullName) return '?'
  return fullName
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')
}

/** Formatea el rol para mostrarlo: "ADMIN" → "Admin" */
function formatRole(role) {
  if (!role) return ''
  return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase()
}

export default function Sidebar() {
  const { currentUser, logout, draftPendingCount } = useAppStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials  = getInitials(currentUser?.fullName)
  const name      = currentUser?.fullName || 'Usuario'
  const roleLabel = formatRole(currentUser?.role)
  const email     = currentUser?.email || ''

  return (
    <nav className={styles.sidebar}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>✦</div>
        <span className={styles.logoText}>Talent<em>Circle</em></span>
      </div>

      <span className={styles.sectionLabel}>Principal</span>
      {NAV.map(({ to, icon: Icon, label }) => {
        // Badge dinámico: solo en /drafts, solo si hay pendientes
        const badge = to === '/drafts' && draftPendingCount > 0 ? draftPendingCount : null
        return (
          <NavLink key={to} to={to} className={({ isActive }) =>
            `${styles.navItem} ${isActive ? styles.active : ''}`}>
            <Icon size={16} />
            <span>{label}</span>
            {badge && <span className={styles.badge}>{badge}</span>}
          </NavLink>
        )
      })}

      <span className={styles.sectionLabel}>Configuración</span>
      {ADMIN_NAV.map(({ to, icon: Icon, label }) => (
        <NavLink key={label} to={to} className={({ isActive }) =>
          `${styles.navItem} ${isActive ? styles.active : ''}`}>
          <Icon size={16} />
          <span>{label}</span>
        </NavLink>
      ))}

      <div className={styles.footer}>
        <div className={styles.userChip}>
          <div className={styles.avatar} title={name}>{initials}</div>
          <div className={styles.userInfo}>
            <div className={styles.userName}>{name}</div>
            <div className={styles.userMeta}>
              <span className={styles.userRole}>{roleLabel}</span>
              {email && <span className={styles.userEmail}>{email}</span>}
            </div>
          </div>
          <button className={styles.logoutBtn} onClick={handleLogout} title="Cerrar sesión">
            <LogOut size={15} />
          </button>
        </div>
      </div>
    </nav>
  )
}
