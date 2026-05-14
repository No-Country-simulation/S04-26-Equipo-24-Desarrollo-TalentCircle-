import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../../store/useAppStore'
import draftsApi from '../../api/draftsApi'
import adminApi from '../../api/adminApi'
import collectorApi from '../../api/collectorApi'
import { derivePendingCount, getMostRecentExecution } from '../../utils/dashboardHelpers'
import styles from './Dashboard.module.css'

// ─── Channel / status display maps ───────────────────────────────────────────
const CHANNEL_LABELS = {
  NEWSLETTER: 'Newsletter',
  LINKEDIN: 'LinkedIn',
  TWITTER: 'Twitter',
}

const STATUS_DISPLAY = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobado',
  PUBLISHED: 'Publicado',
  REJECTED: 'Rechazado',
}

// ─── Pipeline banner — reflects real execution status ─────────────────────────
function PipelineBanner() {
  const { pipelineStatus, pipelineRunning } = useAppStore()

  // Map pipeline status to which step is "active"
  const getStepStatus = (stepIndex) => {
    if (pipelineStatus === 'failed') {
      // Show all steps as waiting on failure
      return stepIndex === 0 ? 'done' : 'waiting'
    }
    if (pipelineRunning || pipelineStatus === 'running') {
      // Animate the step currently in progress
      if (stepIndex === 0) return 'done'
      if (stepIndex === 1) return 'active'
      return 'waiting'
    }
    if (pipelineStatus === 'completed') {
      // All steps done except the last two (editorial + publication are manual)
      if (stepIndex <= 2) return 'done'
      if (stepIndex === 3) return 'active'
      return 'waiting'
    }
    // idle — default view
    if (stepIndex <= 2) return 'done'
    if (stepIndex === 3) return 'active'
    return 'waiting'
  }

  const steps = [
    { label: 'Recolección' },
    { label: 'Análisis IA' },
    { label: 'Generación' },
    { label: 'Revisión' },
    { label: 'Publicación' },
  ]

  const bannerLabel =
    pipelineRunning || pipelineStatus === 'running'
      ? 'Pipeline en ejecución…'
      : pipelineStatus === 'failed'
      ? '⚠ Pipeline fallido'
      : pipelineStatus === 'completed'
      ? '✓ Pipeline completado'
      : 'Pipeline activo'

  return (
    <div className={`${styles.banner} ${pipelineStatus === 'failed' ? styles.bannerError : ''}`}>
      <div className={styles.bannerLeft} />
      <div className={styles.steps}>
        {steps.map((s, i) => {
          const status = getStepStatus(i)
          return (
            <>
              <div key={s.label} className={styles.step}>
                <div className={`${styles.dot} ${styles[status]}`}>
                  {status === 'done' ? '✓' : status === 'active' ? '◉' : '○'}
                </div>
                <span className={styles.stepLabel}>{s.label}</span>
              </div>
              {i < steps.length - 1 && (
                <span className={styles.arrow} key={`a${i}`}>→</span>
              )}
            </>
          )
        })}
      </div>
      <span className={`${styles.bannerTime} ${pipelineStatus === 'failed' ? styles.bannerTimeError : ''}`}>
        {bannerLabel}
      </span>
    </div>
  )
}

// ─── Skeleton for a single stat card ─────────────────────────────────────────
function StatCardSkeleton() {
  return (
    <div className={`${styles.statCard} ${styles.skeletonCard}`}>
      <div className={`${styles.skeletonBlock} ${styles.skeletonLabel}`} />
      <div className={`${styles.skeletonBlock} ${styles.skeletonValue}`} />
      <div className={`${styles.skeletonBlock} ${styles.skeletonChange}`} />
    </div>
  )
}

// ─── Skeleton for a single feed item ─────────────────────────────────────────
function FeedItemSkeleton() {
  return (
    <div className={styles.feedItem}>
      <div className={`${styles.skeletonBlock} ${styles.skeletonAvatar}`} />
      <div className={styles.feedContent}>
        <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonFull}`} />
        <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonMed}`} />
      </div>
    </div>
  )
}

// ─── Skeleton for a single draft summary card ─────────────────────────────────
function DraftSummarySkeleton() {
  return (
    <div className={styles.draftSummary}>
      <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonShort}`} />
      <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonFull}`} />
      <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonMed}`} />
    </div>
  )
}

// ─── Page component ───────────────────────────────────────────────────────────
export default function Dashboard() {
  const { openModal } = useAppStore()
  const navigate = useNavigate()

  // ── Local state ──────────────────────────────────────────────────────────
  const [drafts, setDrafts] = useState([])
  const [activities, setActivities] = useState([])
  const [statsLoading, setStatsLoading] = useState(true)
  const [feedLoading, setFeedLoading] = useState(true)

  // ── Data fetching ────────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false

    // Step 1: fetch drafts and executions in parallel
    Promise.all([draftsApi.list(), adminApi.getExecutions()])
      .then(([fetchedDrafts, executions]) => {
        if (cancelled) return

        setDrafts(fetchedDrafts)
        setStatsLoading(false)

        // Step 2: fetch activities for the most recent execution
        if (executions.length === 0) {
          setFeedLoading(false)
          return
        }

        const mostRecent = getMostRecentExecution(executions)
        collectorApi
          .getActivities(mostRecent.id)
          .then((fetchedActivities) => {
            if (!cancelled) setActivities(fetchedActivities)
          })
          .catch(() => {
            // error already toasted by apiClient interceptor
          })
          .finally(() => {
            if (!cancelled) setFeedLoading(false)
          })
      })
      .catch(() => {
        // error already toasted by apiClient interceptor
        if (!cancelled) {
          setStatsLoading(false)
          setFeedLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  // ── Derived stats ────────────────────────────────────────────────────────
  const generatedCount = drafts.length
  const pendingCount = derivePendingCount(drafts)

  // Three most recent drafts sorted by createdAt descending
  const weekDrafts = [...drafts]
    .sort((a, b) => (b.createdAt > a.createdAt ? 1 : b.createdAt < a.createdAt ? -1 : 0))
    .slice(0, 3)

  // ── Stat card definitions (two are dynamic, two are static placeholders) ─
  const STATS = [
    {
      label: 'Actividades Recolectadas',
      value: activities.length > 0 ? String(activities.length) : '—',
      color: 'amber',
      change: 'Fuentes activas esta semana',
    },
    {
      label: 'Borradores Generados',
      value: String(generatedCount),
      color: 'teal',
      change: 'Total de borradores',
    },
    {
      label: 'Pendientes de Revisión',
      value: String(pendingCount),
      color: 'rose',
      change: 'Requieren acción',
    },
    {
      label: 'Publicados este mes',
      value: '—',
      color: 'green',
      change: 'Ver historial de ejecuciones',
    },
  ]

  return (
    <div className={styles.page}>
      <PipelineBanner />

      {/* Stats grid */}
      <div className={styles.statsGrid} data-testid="stats-grid">
        {statsLoading
          ? Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)
          : STATS.map((s) => (
              <div key={s.label} className={`${styles.statCard} ${styles[s.color]}`}>
                <p className={styles.statLabel}>{s.label}</p>
                <p className={`${styles.statValue} ${styles[s.color + 'Text']}`}>{s.value}</p>
                <p className={styles.statChange}>{s.change}</p>
              </div>
            ))}
      </div>

      {/* Main grid */}
      <div className={styles.grid}>
        {/* Activity feed */}
        <div>
          <div className="section-header">
            <span className="section-title">Actividad Comunitaria Destacada</span>
            <span className="section-link">Ver todo →</span>
          </div>
          <div className={styles.feed} data-testid="activity-feed">
            <div className={styles.feedHeader}>
              <span>📡</span>
              <span>Feed en tiempo real</span>
              <span className={styles.liveDot} />
            </div>

            {feedLoading ? (
              Array.from({ length: 4 }).map((_, i) => <FeedItemSkeleton key={i} />)
            ) : activities.length === 0 ? (
              <div className={styles.feedItem}>
                <p style={{ color: 'var(--text3)', fontSize: '13px', padding: '8px 0' }}>
                  No hay actividad reciente disponible.
                </p>
              </div>
            ) : (
              activities.map((activity) => (
                <div key={activity.id} className={styles.feedItem}>
                  <div
                    className={styles.feedAvatar}
                    style={{ background: 'var(--surface3)', color: 'var(--text2)' }}
                  >
                    {activity.author ? activity.author.slice(0, 2).toUpperCase() : '??'}
                  </div>
                  <div className={styles.feedContent}>
                    <p className={styles.feedText}>
                      <strong>{activity.author}</strong>{' '}
                      {activity.title ? `— ${activity.title}` : ''}
                    </p>
                    {activity.content && (
                      <p className={styles.feedText} style={{ marginTop: 2 }}>
                        {activity.content}
                      </p>
                    )}
                    <div className={styles.feedMeta}>
                      <span>👍 {activity.reactionCount}</span>
                      <span>💬 {activity.responseCount}</span>
                      <span>↗ {activity.shareCount}</span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Draft summaries */}
        <div>
          <div className="section-header">
            <span className="section-title">Borradores de la Semana</span>
            <span className="section-link" onClick={() => navigate('/drafts')}>
              Ver todos →
            </span>
          </div>
          <div className={styles.draftList} data-testid="draft-list">
            {statsLoading ? (
              Array.from({ length: 3 }).map((_, i) => <DraftSummarySkeleton key={i} />)
            ) : weekDrafts.length === 0 ? (
              <p style={{ color: 'var(--text3)', fontSize: '13px' }}>
                No hay borradores esta semana.
              </p>
            ) : (
              weekDrafts.map((d) => {
                const channelKey = (d.channel ?? '').toLowerCase()
                const channelLabel = CHANNEL_LABELS[d.channel] ?? d.channel
                const statusLabel = STATUS_DISPLAY[d.status] ?? d.status
                return (
                  <div
                    key={d.id}
                    className={`${styles.draftSummary} ${styles[channelKey]}`}
                    onClick={() => openModal(d.id)}
                  >
                    <div className={styles.dscTop}>
                      <span className={`channel-badge ${channelKey}`}>{channelLabel}</span>
                      <span className={`status-badge ${d.status}`}>{statusLabel}</span>
                    </div>
                    <p className={styles.dscTitle}>{d.summary}</p>
                    <p className={styles.dscPreview}>{d.summary}</p>
                    <p className={styles.dscScore}>✦ Score IA: {d.aiScore ?? '—'} / 10</p>
                  </div>
                )
              })
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
