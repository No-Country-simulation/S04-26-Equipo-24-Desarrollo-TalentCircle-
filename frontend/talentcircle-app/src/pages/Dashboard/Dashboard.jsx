import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../../store/useAppStore'
import { getLatestExecution, getWeekDrafts } from '../../services/dashboardService'
import styles from './Dashboard.module.css'

// Mapeo de estado de ejecución a pasos del pipeline
const STEP_STATUS_MAP = {
  RUNNING:   ['done', 'active', 'waiting', 'waiting', 'waiting'],
  COMPLETED: ['done', 'done',   'done',    'active',  'waiting'],
  FAILED:    ['done', 'done',   'done',    'waiting', 'waiting'],
}
const STEP_LABELS = ['Recolección', 'Análisis IA', 'Generación', 'Revisión', 'Publicación']

function PipelineBanner({ execution }) {
  const statuses = STEP_STATUS_MAP[execution?.status] ?? STEP_STATUS_MAP.COMPLETED
  const lastRun = execution?.completedAt
    ? new Date(execution.completedAt).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
    : execution?.startedAt
      ? new Date(execution.startedAt).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' })
      : 'Sin ejecuciones'

  return (
    <div className={styles.banner}>
      <div className={styles.bannerLeft} />
      <div className={styles.steps}>
        {STEP_LABELS.map((label, i) => (
          <>
            <div key={label} className={styles.step}>
              <div className={`${styles.dot} ${styles[statuses[i]]}`}>
                {statuses[i] === 'done' ? '✓' : statuses[i] === 'active' ? '◉' : '○'}
              </div>
              <span className={styles.stepLabel}>{label}</span>
            </div>
            {i < STEP_LABELS.length - 1 && <span className={styles.arrow} key={`a${i}`}>→</span>}
          </>
        ))}
      </div>
      <span className={styles.bannerTime}>Última ejecución: {lastRun}</span>
    </div>
  )
}

const MOCK_STATS = [
  { label: 'Actividades Recolectadas', value: '147', color: 'amber', change: '↑ 23% vs semana anterior' },
  { label: 'Borradores Generados',     value: '6',   color: 'teal',  change: '2 canales · 3 formatos' },
  { label: 'Pendientes de Revisión',   value: '4',   color: 'rose',  change: '2 aprobados esta semana' },
  { label: 'Publicados este mes',      value: '18',  color: 'green', change: '↑ 40% alcance estimado' },
]

const CHANNEL_ICON = { LINKEDIN: '🔵', TWITTER: '🐦', NEWSLETTER: '📧', linkedin: '🔵', twitter: '🐦', newsletter: '📧' }
const STATUS_LABEL = { PENDING: 'Pendiente', APPROVED: 'Aprobado', PUBLISHED: 'Publicado', REJECTED: 'Rechazado',
                       pending: 'Pendiente', approved: 'Aprobado', published: 'Publicado', rejected: 'Rechazado' }

export default function Dashboard() {
  const { feedItems, drafts: mockDrafts, openModal } = useAppStore()
  const navigate = useNavigate()

  const [execution, setExecution] = useState(null)
  const [weekDrafts, setWeekDrafts] = useState(mockDrafts.slice(0, 3))
  const [stats, setStats] = useState(MOCK_STATS)
  const [usingBackend, setUsingBackend] = useState(false)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [latestExec, draftsData] = await Promise.all([
          getLatestExecution(),
          getWeekDrafts(),
        ])

        setExecution(latestExec)

        if (draftsData?.length) {
          // Normalizar al formato del store
          const normalized = draftsData.slice(0, 3).map((d) => ({
            id: d.id,
            channel: (d.channel ?? 'newsletter').toLowerCase(),
            channelLabel: d.channelLabel ?? d.channel,
            icon: CHANNEL_ICON[d.channel] ?? '📄',
            title: d.title ?? '',
            preview: d.preview ?? d.content?.slice(0, 120) ?? '',
            status: (d.status ?? 'pending').toLowerCase(),
            score: d.aiScore ?? d.score ?? 0,
            week: d.weekLabel ?? '',
          }))
          setWeekDrafts(normalized)
        }

        // Calcular stats dinámicas desde los datos reales
        if (draftsData?.length) {
          const pending   = draftsData.filter((d) => (d.status ?? '').toLowerCase() === 'pending').length
          const published = draftsData.filter((d) => (d.status ?? '').toLowerCase() === 'published').length
          setStats([
            { label: 'Actividades Recolectadas', value: String(latestExec?.activitiesCount ?? '—'), color: 'amber', change: 'Última ejecución' },
            { label: 'Borradores Generados',     value: String(draftsData.length),                  color: 'teal',  change: '3 canales disponibles' },
            { label: 'Pendientes de Revisión',   value: String(pending),                            color: 'rose',  change: 'Requieren aprobación' },
            { label: 'Publicados',               value: String(published),                          color: 'green', change: 'Esta semana' },
          ])
        }

        setUsingBackend(true)
      } catch {
        // Backend no disponible → usar datos mock
        setUsingBackend(false)
      }
    }
    fetchData()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className={styles.page}>
      {!usingBackend && (
        <div style={{ padding: '8px 16px', background: 'rgba(245,166,35,.12)', borderRadius: 8, marginBottom: 12, fontSize: 12, color: 'var(--amber)' }}>
          ⚠ Modo demo — backend no disponible. Mostrando datos locales.
        </div>
      )}

      <PipelineBanner execution={execution} />

      {/* Stats */}
      <div className={styles.statsGrid}>
        {stats.map((s) => (
          <div key={s.label} className={`${styles.statCard} ${styles[s.color]}`}>
            <p className={styles.statLabel}>{s.label}</p>
            <p className={`${styles.statValue} ${styles[s.color + 'Text']}`}>{s.value}</p>
            <p className={styles.statChange}>{s.change}</p>
          </div>
        ))}
      </div>

      {/* Grid */}
      <div className={styles.grid}>
        {/* Feed — siempre desde el store (datos de comunidad) */}
        <div>
          <div className="section-header">
            <span className="section-title">Actividad Comunitaria Destacada</span>
            <span className="section-link">Ver todo →</span>
          </div>
          <div className={styles.feed}>
            <div className={styles.feedHeader}>
              <span>📡</span><span>Feed en tiempo real</span>
              <span className={styles.liveDot} />
            </div>
            {feedItems.map((f) => (
              <div key={f.id} className={styles.feedItem}>
                <div className={styles.feedAvatar} style={{ background: f.bg, color: f.color }}>{f.initials}</div>
                <div className={styles.feedContent}>
                  <p className={styles.feedText}><strong>{f.author}</strong> {f.text}</p>
                  <div className={styles.feedMeta}>
                    <span>{f.time}</span>
                    <span>{f.reactions}</span>
                    <span style={{ color: 'var(--green)' }}>Score: {f.score}/10</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Borradores de la semana */}
        <div>
          <div className="section-header">
            <span className="section-title">Borradores de la Semana</span>
            <span className="section-link" onClick={() => navigate('/drafts')}>Ver todos →</span>
          </div>
          <div className={styles.draftList}>
            {weekDrafts.map((d) => (
              <div key={d.id} className={`${styles.draftSummary} ${styles[d.channel]}`} onClick={() => openModal(d.id)}>
                <div className={styles.dscTop}>
                  <span className={`channel-badge ${d.channel}`}>{d.channelLabel ?? d.channel}</span>
                  <span className={`status-badge ${d.status}`}>{STATUS_LABEL[d.status] ?? d.status}</span>
                </div>
                <p className={styles.dscTitle}>{d.title}</p>
                <p className={styles.dscPreview}>{d.preview}</p>
                <p className={styles.dscScore}>✦ Score IA: {d.score} / 10</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
