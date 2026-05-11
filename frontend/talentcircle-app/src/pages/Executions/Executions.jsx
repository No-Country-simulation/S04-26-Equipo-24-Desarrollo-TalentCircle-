import { useState, useEffect } from 'react'
import { useAppStore } from '../../store/useAppStore'
import adminApi from '../../api/adminApi'
import styles from './Executions.module.css'

const STATUS_META = {
  COMPLETED: { label: 'Completado', color: 'var(--green)' },
  RUNNING:   { label: 'En curso',   color: 'var(--amber)' },
  FAILED:    { label: 'Fallido',    color: 'var(--rose)'  },
  // lowercase fallbacks for legacy mock data
  completed: { label: 'Completado', color: 'var(--green)' },
  running:   { label: 'En curso',   color: 'var(--amber)' },
  failed:    { label: 'Fallido',    color: 'var(--rose)'  },
}

// ─── Skeleton row shown while loading ────────────────────────────────────────
function SkeletonRow() {
  return (
    <tr className={styles.row}>
      {Array.from({ length: 6 }).map((_, i) => (
        <td key={i} className={styles.td}>
          <div className={styles.skeletonBlock} style={{ width: i === 0 ? 80 : i === 2 ? 90 : 120, height: 14 }} />
        </td>
      ))}
    </tr>
  )
}

// ─── Page component ───────────────────────────────────────────────────────────
export default function Executions() {
  const { showToast, currentUser } = useAppStore()

  const [executions, setExecutions] = useState([])
  const [loading, setLoading]       = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  // Fetch executions on mount with cancelled-flag pattern
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    adminApi
      .getExecutions()
      .then((result) => { if (!cancelled) setExecutions(result) })
      .catch(() => { /* error already toasted by apiClient interceptor */ })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  // Trigger a manual execution
  const handleTrigger = async () => {
    setActionLoading(true)
    try {
      await adminApi.triggerExecution(currentUser?.email ?? '')
      showToast('🚀', 'Ejecución iniciada', 'La ejecución manual fue disparada exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      {/* Header with trigger button */}
      <div className={styles.pageHeader}>
        <button
          className={styles.triggerBtn}
          onClick={handleTrigger}
          disabled={actionLoading}
          data-testid="trigger-btn"
        >
          {actionLoading ? (
            <>
              <span className={styles.spinner} />
              Disparando…
            </>
          ) : (
            '▶ Disparar ejecución manual'
          )}
        </button>
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              {['ID Ejecución', 'Semana inicio', 'Semana fin', 'Estado', 'Iniciado', 'Completado'].map((h) => (
                <th key={h} className={styles.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} />)
            ) : executions.length === 0 ? (
              <tr>
                <td colSpan={6} className={styles.emptyCell}>
                  No hay ejecuciones registradas
                </td>
              </tr>
            ) : (
              executions.map((ex) => {
                const sm = STATUS_META[ex.status] ?? { label: ex.status, color: 'var(--text3)' }
                return (
                  <tr key={ex.id} className={styles.row}>
                    <td className={styles.tdId}>#{ex.id}</td>
                    <td className={styles.td}>{ex.weekStart}</td>
                    <td className={styles.td}>{ex.weekEnd}</td>
                    <td className={styles.td}>
                      <span className={styles.statusChip}>
                        <span
                          className={styles.statusDot}
                          style={{
                            background: sm.color,
                            boxShadow: ex.status === 'RUNNING' || ex.status === 'running'
                              ? `0 0 6px ${sm.color}`
                              : 'none',
                          }}
                        />
                        {sm.label}
                      </span>
                    </td>
                    <td className={styles.tdDuration}>{ex.startedAt}</td>
                    <td className={styles.td}>{ex.completedAt ?? '—'}</td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
