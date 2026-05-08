import { useState, useEffect } from 'react'
import { CheckCircle, XCircle, Send, Search } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import { listDrafts, approveDraft, rejectDraft, publishDraft } from '../../services/draftService'
import styles from './Drafts.module.css'

const CHANNELS = ['Todos', 'linkedin', 'twitter', 'newsletter']
const STATUSES = ['Todos', 'pending', 'approved', 'published', 'rejected']
const CHANNEL_LABELS = { linkedin: 'LinkedIn', twitter: 'Twitter', newsletter: 'Newsletter' }
const STATUS_DISPLAY = { pending: 'Pendiente', approved: 'Aprobado', published: 'Publicado', rejected: 'Rechazado' }

// Mapeo de canal a emoji para normalizar datos del backend
const CHANNEL_ICON = { linkedin: '🔵', twitter: '🐦', newsletter: '📧', LINKEDIN: '🔵', TWITTER: '🐦', NEWSLETTER: '📧' }

function DraftCard({ draft, onStatusChange }) {
  const { openModal, updateDraftStatus, showToast } = useAppStore()
  const [publishing, setPublishing] = useState(false)
  const pct = Math.round((draft.score ?? 0) * 10)

  const approve = async (e) => {
    e.stopPropagation()
    try {
      await approveDraft(draft.id)
    } catch {
      // backend no disponible → actualizar localmente
    }
    updateDraftStatus(draft.id, 'approved')
    showToast('✅', 'Borrador aprobado', 'Listo para publicación')
    onStatusChange?.()
  }

  const reject = async (e) => {
    e.stopPropagation()
    const reason = 'Rechazado desde el panel'
    try {
      await rejectDraft(draft.id, reason)
    } catch {
      // backend no disponible → actualizar localmente
    }
    updateDraftStatus(draft.id, 'rejected', { rejectionReason: reason })
    showToast('✗', 'Borrador rechazado', 'Registrado en el sistema')
    onStatusChange?.()
  }

  const publish = async (e) => {
    e.stopPropagation()
    setPublishing(true)
    showToast('🚀', `Publicando en ${draft.channelLabel}…`, 'Conectando con la API…')
    try {
      const result = await publishDraft(draft.id)
      if (result.status === 'SUCCESS') {
        updateDraftStatus(draft.id, 'published', { publishedAt: result.publishedAt ?? 'ahora' })
        showToast('✅', '¡Publicado!', `Post visible en ${draft.channelLabel}`)
      } else {
        // El backend respondió pero con error (FAILED)
        showToast('⚠️', 'Error al publicar', result.errorMessage ?? 'Revisa la configuración del canal')
      }
    } catch (err) {
      if (!err.response) {
        // Backend no disponible → simular publicación local
        updateDraftStatus(draft.id, 'published', { publishedAt: 'ahora' })
        showToast('✅', '¡Publicado! (demo)', `Backend no disponible — estado actualizado localmente`)
      } else {
        const msg = err.response?.data?.message ?? err.message ?? 'Error desconocido'
        showToast('⚠️', 'Error al publicar', msg)
      }
    } finally {
      setPublishing(false)
      onStatusChange?.()
    }
  }

  return (
    <div className={styles.card} onClick={() => openModal(draft.id)}>
      <div className={styles.cardHeader}>
        <span className={styles.chanIcon}>{draft.icon ?? CHANNEL_ICON[draft.channel] ?? '📄'}</span>
        <div className={styles.chanMeta}>
          <span className={styles.chanName}>{(draft.channelLabel ?? draft.channel ?? '').toUpperCase()}</span>
          <span className={styles.chanDate}>{draft.createdAt}</span>
        </div>
        <span className={`status-badge ${draft.status}`}>{STATUS_DISPLAY[draft.status] ?? draft.status}</span>
      </div>
      <div className={styles.cardBody}>
        <h3 className={styles.cardTitle}>{draft.title}</h3>
        <p className={styles.cardPreview}>{draft.preview}</p>
      </div>
      <div className={styles.cardFooter}>
        <div className={styles.scoreBar}>
          <div className={styles.scoreTrack}>
            <div className={styles.scoreFill} style={{ width: `${pct}%` }} />
          </div>
          <span className={styles.scoreVal}>✦ {draft.score}</span>
        </div>
        <div className={styles.cardActions} onClick={(e) => e.stopPropagation()}>
          {draft.status === 'pending' && <>
            <button className="btn-sm approve" onClick={approve}><CheckCircle size={11} /> Aprobar</button>
            <button className="btn-sm reject" onClick={reject}><XCircle size={11} /> Rechazar</button>
          </>}
          {draft.status === 'approved' && (
            <button className="btn-sm publish" onClick={publish} disabled={publishing}>
              {publishing
                ? <span style={{ fontSize: 10 }}>Publicando…</span>
                : <><Send size={11} /> Publicar</>
              }
            </button>
          )}
          {draft.status === 'rejected' && (
            <button className="btn-sm approve" onClick={approve}>↺ Revisar</button>
          )}
          {draft.status === 'published' && (
            <span className={styles.pubLabel}>↑ Publicado · {draft.publishedAt}</span>
          )}
        </div>
      </div>
    </div>
  )
}

export default function Drafts() {
  const { drafts: mockDrafts } = useAppStore()
  const [drafts, setDrafts] = useState(mockDrafts)
  const [search, setSearch] = useState('')
  const [channel, setChannel] = useState('Todos')
  const [status, setStatus] = useState('Todos')
  const [loading, setLoading] = useState(false)
  const [usingBackend, setUsingBackend] = useState(false)

  const fetchDrafts = async () => {
    setLoading(true)
    try {
      const params = {}
      if (channel !== 'Todos') params.channel = channel.toUpperCase()
      if (status !== 'Todos') params.status = status.toUpperCase()
      const data = await listDrafts(params)
      const normalized = data.map((d) => ({
        id: d.id,
        channel: (d.channel ?? 'newsletter').toLowerCase(),
        channelLabel: d.channelLabel ?? d.channel,
        icon: d.icon ?? CHANNEL_ICON[d.channel] ?? '📄',
        title: d.title,
        preview: d.preview ?? d.content?.slice(0, 120) ?? '',
        content: d.content ?? '',
        status: (d.status ?? 'pending').toLowerCase(),
        score: d.aiScore ?? d.score ?? 0,
        createdAt: d.createdAt ?? '',
        week: d.weekLabel ?? '',
        sources: d.sources ?? [],
        publishedAt: d.publishedAt ?? null,
        rejectionReason: d.rejectionReason ?? null,
      }))
      setDrafts(normalized)
      setUsingBackend(true)
    } catch {
      setDrafts(mockDrafts)
      setUsingBackend(false)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchDrafts() }, [channel, status]) // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = drafts.filter((d) => {
    const matchChan = channel === 'Todos' || d.channel === channel
    const matchStatus = status === 'Todos' || d.status === status
    const matchSearch = d.title.toLowerCase().includes(search.toLowerCase()) ||
      d.preview.toLowerCase().includes(search.toLowerCase())
    return matchChan && matchStatus && matchSearch
  })

  return (
    <div className={styles.page}>
      {!usingBackend && (
        <div style={{ padding: '8px 16px', background: 'rgba(245,166,35,.12)', borderRadius: 8, marginBottom: 12, fontSize: 12, color: 'var(--amber)' }}>
          ⚠ Modo demo — backend no disponible. Mostrando datos locales.
        </div>
      )}
      <div className={styles.filtersBar}>
        <div className={styles.searchWrap}>
          <Search size={14} className={styles.searchIcon} />
          <input className={styles.search} placeholder="Buscar en borradores…" value={search}
            onChange={(e) => setSearch(e.target.value)} />
        </div>
        {CHANNELS.map((c) => (
          <button key={c} className={`${styles.chip} ${channel === c ? styles.active : ''}`} onClick={() => setChannel(c)}>
            {CHANNEL_LABELS[c] || c}
          </button>
        ))}
        <div className={styles.divider} />
        {STATUSES.map((s) => (
          <button key={s} className={`${styles.chip} ${status === s ? styles.active : ''}`} onClick={() => setStatus(s)}>
            {STATUS_DISPLAY[s] || s}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="empty-state"><span className="icon">⏳</span><p>Cargando borradores…</p></div>
      ) : filtered.length === 0 ? (
        <div className="empty-state"><span className="icon">🔍</span><p>No hay borradores que coincidan con los filtros</p></div>
      ) : (
        <div className={styles.grid}>
          {filtered.map((d) => <DraftCard key={d.id} draft={d} onStatusChange={fetchDrafts} />)}
        </div>
      )}
    </div>
  )
}
