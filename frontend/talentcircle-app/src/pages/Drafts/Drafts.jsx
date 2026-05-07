import { useState, useEffect } from 'react'
import { CheckCircle, XCircle, Search } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import draftsApi from '../../api/draftsApi'
import styles from './Drafts.module.css'

// Map API channel enum → display label
const CHANNEL_LABELS = {
  NEWSLETTER: 'Newsletter',
  LINKEDIN: 'LinkedIn',
  TWITTER: 'Twitter',
}

// Map API status enum → display label
const STATUS_DISPLAY = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobado',
  PUBLISHED: 'Publicado',
  REJECTED: 'Rechazado',
}

// Filter options (first entry = "no filter")
const CHANNEL_FILTERS = ['Todos', 'NEWSLETTER', 'LINKEDIN', 'TWITTER']
const STATUS_FILTERS  = ['Todos', 'PENDING', 'APPROVED', 'PUBLISHED', 'REJECTED']

// ─── Skeleton card shown while loading ───────────────────────────────────────
function SkeletonCard() {
  return (
    <div className={styles.skeletonCard}>
      <div className={styles.skeletonHeader}>
        <div className={`${styles.skeletonBlock} ${styles.skeletonIcon}`} />
        <div className={styles.skeletonMeta}>
          <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonShort}`} />
          <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonXshort}`} />
        </div>
        <div className={`${styles.skeletonBlock} ${styles.skeletonBadge}`} />
      </div>
      <div className={styles.skeletonBody}>
        <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonFull}`} />
        <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonFull}`} />
        <div className={`${styles.skeletonBlock} ${styles.skeletonLine} ${styles.skeletonMed}`} />
      </div>
      <div className={styles.skeletonFooter}>
        <div className={`${styles.skeletonBlock} ${styles.skeletonScore}`} />
        <div className={styles.skeletonActions}>
          <div className={`${styles.skeletonBlock} ${styles.skeletonBtn}`} />
          <div className={`${styles.skeletonBlock} ${styles.skeletonBtn}`} />
        </div>
      </div>
    </div>
  )
}

// ─── Individual draft card ────────────────────────────────────────────────────
function DraftCard({ draft, onStatusChange }) {
  const { openModal, updateDraftStatus, showToast } = useAppStore()
  const [actionLoading, setActionLoading] = useState(false)

  const pct = draft.aiScore != null ? Math.round(draft.aiScore * 10) : 0

  const handleApprove = async (e) => {
    e.stopPropagation()
    setActionLoading(true)
    try {
      await draftsApi.approve(draft.id)
      updateDraftStatus(draft.id, 'APPROVED')
      onStatusChange && onStatusChange()
      showToast('✅', 'Borrador aprobado', 'El borrador fue aprobado exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleReject = async (e) => {
    e.stopPropagation()
    setActionLoading(true)
    try {
      await draftsApi.reject(draft.id, 'Rechazado desde el panel')
      updateDraftStatus(draft.id, 'REJECTED')
      onStatusChange && onStatusChange()
      showToast('✗', 'Borrador rechazado', 'El borrador fue rechazado exitosamente.')
    } finally {
      setActionLoading(false)
    }
  }

  const channelLabel = CHANNEL_LABELS[draft.channel] ?? draft.channel
  const statusLabel  = STATUS_DISPLAY[draft.status]  ?? draft.status

  return (
    <div className={styles.card} onClick={() => openModal(draft.id)}>
      <div className={styles.cardHeader}>
        <div className={styles.chanMeta}>
          <span className={styles.chanName}>{channelLabel.toUpperCase()}</span>
          <span className={styles.chanDate}>{draft.createdAt}</span>
        </div>
        <span className={`status-badge ${draft.status}`}>{statusLabel}</span>
      </div>
      <div className={styles.cardBody}>
        <p className={styles.cardPreview}>{draft.summary}</p>
      </div>
      <div className={styles.cardFooter}>
        <div className={styles.scoreBar}>
          <div className={styles.scoreTrack}>
            <div className={styles.scoreFill} style={{ width: `${pct}%` }} />
          </div>
          <span className={styles.scoreVal}>✦ {draft.aiScore ?? '—'}</span>
        </div>
        <div className={styles.cardActions} onClick={(e) => e.stopPropagation()}>
          {draft.status === 'PENDING' && (
            <>
              <button
                className={`${styles['btn-sm']} ${styles.approve}`}
                onClick={handleApprove}
                disabled={actionLoading}
              >
                <CheckCircle size={11} />
                {actionLoading ? '…' : 'Aprobar'}
              </button>
              <button
                className={`${styles['btn-sm']} ${styles.reject}`}
                onClick={handleReject}
                disabled={actionLoading}
              >
                <XCircle size={11} />
                {actionLoading ? '…' : 'Rechazar'}
              </button>
            </>
          )}
          {draft.status === 'REJECTED' && (
            <button
              className={`${styles['btn-sm']} ${styles.approve}`}
              onClick={handleApprove}
              disabled={actionLoading}
            >
              {actionLoading ? '…' : '↺ Revisar'}
            </button>
          )}
          {draft.status === 'PUBLISHED' && (
            <span className={styles.pubLabel}>↑ Publicado</span>
          )}
        </div>
      </div>
    </div>
  )
}

// ─── Page component ───────────────────────────────────────────────────────────
export default function Drafts() {
  const { drafts, setDrafts } = useAppStore()

  const [loading, setLoading]   = useState(true)
  const [search, setSearch]     = useState('')
  const [channel, setChannel]   = useState('Todos')
  const [status, setStatus]     = useState('Todos')

  // Build filter params for the API call (omit "Todos" = no filter)
  const buildFilters = () => {
    const filters = {}
    if (channel !== 'Todos') filters.channel = channel
    if (status  !== 'Todos') filters.status  = status
    return filters
  }

  const fetchDrafts = () => {
    let cancelled = false
    setLoading(true)
    draftsApi
      .list(buildFilters())
      .then((result) => { if (!cancelled) setDrafts(result) })
      .catch(() => { /* error already toasted by apiClient interceptor */ })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }

  // Fetch on mount
  useEffect(() => {
    const cleanup = fetchDrafts()
    return cleanup
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Re-fetch when channel or status filter changes
  useEffect(() => {
    const cleanup = fetchDrafts()
    return cleanup
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channel, status])

  // Client-side search filter (applied on top of server-side channel/status)
  const filtered = drafts.filter((d) => {
    if (!search) return true
    const q = search.toLowerCase()
    return (
      (d.summary ?? '').toLowerCase().includes(q) ||
      (CHANNEL_LABELS[d.channel] ?? d.channel ?? '').toLowerCase().includes(q)
    )
  })

  return (
    <div className={styles.page}>
      {/* Filters bar */}
      <div className={styles.filtersBar}>
        <div className={styles.searchWrap}>
          <Search size={14} className={styles.searchIcon} />
          <input
            className={styles.search}
            placeholder="Buscar en borradores…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {CHANNEL_FILTERS.map((c) => (
          <button
            key={c}
            className={`${styles.chip} ${channel === c ? styles.active : ''}`}
            onClick={() => setChannel(c)}
          >
            {CHANNEL_LABELS[c] ?? c}
          </button>
        ))}

        <div className={styles.divider} />

        {STATUS_FILTERS.map((s) => (
          <button
            key={s}
            className={`${styles.chip} ${status === s ? styles.active : ''}`}
            onClick={() => setStatus(s)}
          >
            {STATUS_DISPLAY[s] ?? s}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? (
        <div className={styles.grid}>
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <span className="icon">🔍</span>
          <p>No hay borradores que coincidan con los filtros</p>
        </div>
      ) : (
        <div className={styles.grid}>
          {filtered.map((d) => (
            <DraftCard key={d.id} draft={d} onStatusChange={fetchDrafts} />
          ))}
        </div>
      )}
    </div>
  )
}
