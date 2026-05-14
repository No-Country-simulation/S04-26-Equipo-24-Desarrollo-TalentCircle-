import { useLocation, useNavigate } from 'react-router-dom'
import { Bell, Play, MoreVertical, AlertTriangle, X } from 'lucide-react'
import { useAppStore } from '../store/useAppStore'
import adminApi from '../api/adminApi'
import styles from './Topbar.module.css'

const META = {
  '/dashboard':  { title: 'Dashboard Semanal',       sub: 'Resumen semanal del pipeline de contenido' },
  '/drafts':     { title: 'Borradores',               sub: 'Panel editorial — revisión y aprobación' },
  '/executions': { title: 'Historial de Ejecuciones', sub: 'Registro completo del pipeline' },
  '/admin':      { title: 'Administración',            sub: 'Configuración del sistema y usuarios' },
}

export default function Topbar() {
  const { pathname } = useLocation()
  const navigate = useNavigate()

  const {
    showToast,
    pipelineRunning,
    pipelineStatus,
    pipelineError,
    pipelineAlertDismissed,
    setPipelineRunning,
    setPipelineStatus,
    dismissPipelineAlert,
  } = useAppStore()

  const { title, sub } = META[pathname] || { title: 'TalentCircle', sub: '' }

  // ── Show alert bell when last execution failed and user hasn't dismissed it
  const showAlert =
    pipelineStatus === 'failed' && !pipelineAlertDismissed

  // ── Trigger pipeline manually ─────────────────────────────────────────────
  const runPipeline = async () => {
    if (pipelineRunning) return

    setPipelineRunning(true)
    setPipelineStatus('running')
    showToast('⚙️', 'Pipeline iniciado', 'Recolectando actividad de la comunidad…')

    try {
      const { executionId } = await adminApi.triggerExecution()
      setPipelineStatus('completed', null, executionId)
      showToast('✅', 'Pipeline completado', `Ejecución ${executionId} finalizada. Borradores listos.`)
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Error desconocido al ejecutar el pipeline.'
      setPipelineStatus('failed', msg)
      // Don't call showToast here — apiClient interceptor already showed it
    } finally {
      setPipelineRunning(false)
    }
  }

  return (
    <header className={styles.topbar}>
      <div>
        <h2 className={styles.title}>{title}</h2>
        <p className={styles.sub}>{sub}</p>
      </div>

      <div className={styles.actions}>
        {/* ── Manual pipeline trigger button ── */}
        <button
          className={`${styles.btnRun} ${pipelineRunning ? styles.btnRunning : ''}`}
          onClick={runPipeline}
          disabled={pipelineRunning}
          aria-label="Ejecutar pipeline manualmente"
        >
          {pipelineRunning ? (
            <>
              <span className={styles.spinner} />
              Ejecutando…
            </>
          ) : (
            <>
              <span className={styles.pulse} />
              <Play size={12} fill="currentColor" /> Ejecutar Pipeline
            </>
          )}
        </button>

        {/* ── Alert bell — only visible when last execution failed ── */}
        <div className={styles.bellWrapper}>
          <button
            className={`${styles.iconBtn} ${showAlert ? styles.iconBtnAlert : ''}`}
            onClick={() => {
              if (showAlert) {
                // Navigate to executions so the user can see the error detail
                navigate('/executions')
              } else {
                showToast('🔔', 'Sin alertas', 'El pipeline está funcionando correctamente.')
              }
            }}
            aria-label={showAlert ? 'Ver error del pipeline' : 'Notificaciones'}
          >
            <Bell size={16} />
            {showAlert && <span className={styles.alertDot} aria-hidden="true" />}
          </button>

          {/* ── Inline alert tooltip ── */}
          {showAlert && (
            <div className={styles.alertPopover} role="alert">
              <AlertTriangle size={14} className={styles.alertIcon} />
              <div className={styles.alertBody}>
                <p className={styles.alertTitle}>Pipeline fallido</p>
                <p className={styles.alertMsg}>{pipelineError ?? 'Revisa el historial de ejecuciones.'}</p>
              </div>
              <button
                className={styles.alertClose}
                onClick={(e) => { e.stopPropagation(); dismissPipelineAlert() }}
                aria-label="Cerrar alerta"
              >
                <X size={12} />
              </button>
            </div>
          )}
        </div>

        <button className={styles.iconBtn} aria-label="Más opciones">
          <MoreVertical size={16} />
        </button>
      </div>
    </header>
  )
}
