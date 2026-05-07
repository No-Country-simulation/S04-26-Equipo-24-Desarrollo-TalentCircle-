import { useState, useEffect } from 'react'
import { Save, Plus, ToggleLeft, ToggleRight } from 'lucide-react'
import { useAppStore } from '../../store/useAppStore'
import adminApi from '../../api/adminApi'
import styles from './Admin.module.css'

// ─── Source type icon map ─────────────────────────────────────────────────────
const SOURCE_ICON = { DISCORD: '💬', CIRCLE: '◎', SLACK: '⬡' }
const SOURCE_TYPE_CLASS = { DISCORD: 'discord', CIRCLE: 'circle', SLACK: 'slack' }

const PROMPT_TABS = ['newsletter', 'linkedin', 'twitter']
const PROMPT_LABEL = { newsletter: '📧 Newsletter', linkedin: '🔵 LinkedIn', twitter: '🐦 Twitter' }

// ─── Skeleton helpers ─────────────────────────────────────────────────────────
function SkeletonBlock({ width = 120, height = 14 }) {
  return <div className={styles.skeletonBlock} style={{ width, height }} />
}

function SkeletonSourceRow() {
  return (
    <div className={styles.sourceRow}>
      <div className={styles.skeletonBlock} style={{ width: 36, height: 36, borderRadius: 10 }} />
      <div className={styles.sourceInfo}>
        <SkeletonBlock width={180} height={13} />
        <div style={{ marginTop: 4 }}><SkeletonBlock width={120} height={11} /></div>
      </div>
      <SkeletonBlock width={36} height={22} />
    </div>
  )
}

function SkeletonUserRow() {
  return (
    <tr className={styles.urow}>
      {[140, 180, 60, 60, 80, 60].map((w, i) => (
        <td key={i} className={styles.utd}>
          <SkeletonBlock width={w} height={13} />
        </td>
      ))}
    </tr>
  )
}

// ─── Toggle component ─────────────────────────────────────────────────────────
function Toggle({ active, onChange, disabled }) {
  return (
    <button
      className={`${styles.toggle} ${active ? styles.toggleOn : styles.toggleOff}`}
      onClick={onChange}
      disabled={disabled}
    >
      {active ? <ToggleRight size={22} /> : <ToggleLeft size={22} />}
    </button>
  )
}

// ─── New User Modal ───────────────────────────────────────────────────────────
function NewUserModal({ onClose, onSave }) {
  const [form, setForm] = useState({ fullName: '', email: '', role: 'EDITOR', active: true })
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await onSave(form)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3 className={styles.modalTitle}>Nuevo usuario</h3>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Nombre completo</label>
            <input
              required
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              placeholder="Nombre Apellido"
            />
          </div>
          <div className="field">
            <label>Email</label>
            <input
              required
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="usuario@talentcircle.com"
            />
          </div>
          <div className="field">
            <label>Rol</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="EDITOR">EDITOR</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div className={styles.modalActions}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-green" disabled={saving}>
              {saving ? <><span className={styles.spinner} /> Guardando…</> : 'Crear usuario'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ─── Edit User Modal ──────────────────────────────────────────────────────────
function EditUserModal({ user, onClose, onSave }) {
  const [form, setForm] = useState({ fullName: user.fullName, email: user.email, role: user.role, active: user.active })
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await onSave(user.id, form)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3 className={styles.modalTitle}>Editar usuario</h3>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Nombre completo</label>
            <input
              required
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Email</label>
            <input
              required
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Rol</label>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="EDITOR">EDITOR</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div className="field">
            <label>Estado</label>
            <select
              value={form.active ? 'true' : 'false'}
              onChange={(e) => setForm({ ...form, active: e.target.value === 'true' })}
            >
              <option value="true">Activo</option>
              <option value="false">Inactivo</option>
            </select>
          </div>
          <div className={styles.modalActions}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-green" disabled={saving}>
              {saving ? <><span className={styles.spinner} /> Guardando…</> : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ─── New Source Modal ─────────────────────────────────────────────────────────
function NewSourceModal({ onClose, onSave }) {
  const [form, setForm] = useState({ name: '', type: 'DISCORD', active: true })
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await onSave(form)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3 className={styles.modalTitle}>Agregar fuente</h3>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Nombre</label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Nombre de la fuente"
            />
          </div>
          <div className="field">
            <label>Tipo</label>
            <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              <option value="DISCORD">Discord</option>
              <option value="CIRCLE">Circle</option>
              <option value="SLACK">Slack</option>
            </select>
          </div>
          <div className={styles.modalActions}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-green" disabled={saving}>
              {saving ? <><span className={styles.spinner} /> Guardando…</> : 'Agregar fuente'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ─── Page component ───────────────────────────────────────────────────────────
export default function Admin() {
  const showToast = useAppStore((s) => s.showToast)

  // ── Per-section data & loading states ──────────────────────────────────────
  const [users, setUsers]               = useState([])
  const [usersLoading, setUsersLoading] = useState(true)

  const [sources, setSources]               = useState([])
  const [sourcesLoading, setSourcesLoading] = useState(true)

  const [config, setConfig]               = useState(null)
  const [configLoading, setConfigLoading] = useState(true)

  // ── Action loading states ───────────────────────────────────────────────────
  const [savingConfig, setSavingConfig]   = useState(false)
  const [savingPrompt, setSavingPrompt]   = useState(false)
  const [togglingId, setTogglingId]       = useState(null)

  // ── UI state ────────────────────────────────────────────────────────────────
  const [promptTab, setPromptTab]         = useState('newsletter')
  const [prompts, setPrompts]             = useState({ newsletter: '', linkedin: '', twitter: '' })
  const [llmProvider, setLlmProvider]     = useState('')
  const [llmModel, setLlmModel]           = useState('')
  const [maxItems, setMaxItems]           = useState(20)
  const [cron, setCron]                   = useState('0 18 * * FRI')

  // ── Modal state ─────────────────────────────────────────────────────────────
  const [showNewUser, setShowNewUser]     = useState(false)
  const [editingUser, setEditingUser]     = useState(null)
  const [showNewSource, setShowNewSource] = useState(false)

  // ── Initial parallel fetch ──────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false

    setUsersLoading(true)
    setSourcesLoading(true)
    setConfigLoading(true)

    const usersPromise = adminApi.getUsers()
      .then((data) => { if (!cancelled) setUsers(data) })
      .catch(() => { /* toasted by interceptor */ })
      .finally(() => { if (!cancelled) setUsersLoading(false) })

    const sourcesPromise = adminApi.getSources()
      .then((data) => { if (!cancelled) setSources(data) })
      .catch(() => { /* toasted by interceptor */ })
      .finally(() => { if (!cancelled) setSourcesLoading(false) })

    const configPromise = adminApi.getConfig()
      .then((data) => {
        if (!cancelled) {
          setConfig(data)
          setPrompts({
            newsletter: data.newsletterPrompt ?? '',
            linkedin:   data.linkedinPrompt   ?? '',
            twitter:    data.twitterPrompt    ?? '',
          })
          setLlmProvider(data.llmProvider ?? '')
          setLlmModel(data.llmModel ?? '')
          setMaxItems(data.maxItemsPerChannel ?? 20)
          setCron(data.scheduleCron ?? '0 18 * * FRI')
        }
      })
      .catch(() => { /* toasted by interceptor */ })
      .finally(() => { if (!cancelled) setConfigLoading(false) })

    Promise.all([usersPromise, sourcesPromise, configPromise])

    return () => { cancelled = true }
  }, [])

  // ── Source toggle ───────────────────────────────────────────────────────────
  const handleToggleSource = async (id, currentActive) => {
    setTogglingId(id)
    try {
      const updated = await adminApi.updateSource(id, { active: !currentActive })
      setSources((ss) => ss.map((s) => s.id === id ? { ...s, active: updated.active } : s))
    } finally {
      setTogglingId(null)
    }
  }

  // ── Save pipeline config ────────────────────────────────────────────────────
  const handleSaveConfig = async () => {
    setSavingConfig(true)
    try {
      const payload = {
        ...(config ?? {}),
        llmProvider,
        llmModel,
        maxItemsPerChannel: Number(maxItems),
        scheduleCron: cron,
        newsletterPrompt: prompts.newsletter,
        linkedinPrompt:   prompts.linkedin,
        twitterPrompt:    prompts.twitter,
      }
      const updated = await adminApi.updateConfig(payload)
      setConfig(updated)
      showToast('✅', 'Configuración guardada', 'Aplica en la próxima ejecución')
    } finally {
      setSavingConfig(false)
    }
  }

  // ── Save prompt ─────────────────────────────────────────────────────────────
  const handleSavePrompt = async () => {
    setSavingPrompt(true)
    try {
      const promptField = `${promptTab}Prompt`
      const payload = {
        ...(config ?? {}),
        llmProvider,
        llmModel,
        maxItemsPerChannel: Number(maxItems),
        scheduleCron: cron,
        newsletterPrompt: prompts.newsletter,
        linkedinPrompt:   prompts.linkedin,
        twitterPrompt:    prompts.twitter,
        [promptField]: prompts[promptTab],
      }
      const updated = await adminApi.updateConfig(payload)
      setConfig(updated)
      showToast('✅', 'Prompt guardado', 'Se usará en la próxima ejecución')
    } finally {
      setSavingPrompt(false)
    }
  }

  // ── Create user ─────────────────────────────────────────────────────────────
  const handleCreateUser = async (data) => {
    await adminApi.createUser(data)
    const refreshed = await adminApi.getUsers()
    setUsers(refreshed)
    showToast('✅', 'Usuario creado', `${data.fullName} fue agregado exitosamente`)
  }

  // ── Update user ─────────────────────────────────────────────────────────────
  const handleUpdateUser = async (id, data) => {
    await adminApi.updateUser(id, data)
    const refreshed = await adminApi.getUsers()
    setUsers(refreshed)
    showToast('✅', 'Usuario actualizado', 'Los cambios fueron guardados')
  }

  // ── Create source ───────────────────────────────────────────────────────────
  const handleCreateSource = async (data) => {
    await adminApi.createSource(data)
    const refreshed = await adminApi.getSources()
    setSources(refreshed)
    showToast('✅', 'Fuente agregada', `${data.name} fue agregada exitosamente`)
  }

  // ── Derive initials from fullName ───────────────────────────────────────────
  const getInitials = (fullName = '') =>
    fullName.split(' ').slice(0, 2).map((w) => w[0] ?? '').join('').toUpperCase()

  return (
    <div className={styles.page}>
      <div className={styles.grid}>

        {/* ── Sources ──────────────────────────────────────────────────────── */}
        <div className={styles.card} data-testid="sources-section">
          <h3 className={styles.cardTitle}>📡 Fuentes Comunitarias</h3>
          {sourcesLoading ? (
            <>
              <SkeletonSourceRow />
              <SkeletonSourceRow />
              <SkeletonSourceRow />
            </>
          ) : (
            sources.map((s) => (
              <div key={s.id} className={styles.sourceRow}>
                <div className={`${styles.sourceIcon} ${styles[SOURCE_TYPE_CLASS[s.type] ?? s.type?.toLowerCase()]}`}>
                  {SOURCE_ICON[s.type] ?? '🔗'}
                </div>
                <div className={styles.sourceInfo}>
                  <h4>{s.name}</h4>
                  <span>{s.type} · {s.active ? 'Activo' : 'Inactivo'}</span>
                </div>
                <Toggle
                  active={s.active}
                  onChange={() => handleToggleSource(s.id, s.active)}
                  disabled={togglingId === s.id}
                />
              </div>
            ))
          )}
          <button
            className={styles.addBtn}
            onClick={() => setShowNewSource(true)}
            data-testid="add-source-btn"
          >
            <Plus size={14} /> Agregar fuente
          </button>
        </div>

        {/* ── Pipeline Config ───────────────────────────────────────────────── */}
        <div className={styles.card} data-testid="config-section">
          <h3 className={styles.cardTitle}>⚙ Configuración del Pipeline</h3>
          {configLoading ? (
            <>
              <div className="field"><SkeletonBlock width={200} height={32} /></div>
              <div className="field"><SkeletonBlock width={200} height={32} /></div>
              <div className="field"><SkeletonBlock width={200} height={32} /></div>
              <SkeletonBlock width="100%" height={36} />
            </>
          ) : (
            <>
              <div className="field">
                <label>Proveedor LLM</label>
                <input
                  value={llmProvider}
                  onChange={(e) => setLlmProvider(e.target.value)}
                  placeholder="openai / anthropic"
                />
              </div>
              <div className="field">
                <label>Modelo LLM</label>
                <input
                  value={llmModel}
                  onChange={(e) => setLlmModel(e.target.value)}
                  placeholder="gpt-4o / claude-3-5-sonnet"
                />
              </div>
              <div className="field">
                <label>Máx. actividades por ejecución</label>
                <input
                  type="number"
                  value={maxItems}
                  onChange={(e) => setMaxItems(e.target.value)}
                />
              </div>
              <div className="field">
                <label>Cron de ejecución automática</label>
                <input
                  type="text"
                  value={cron}
                  onChange={(e) => setCron(e.target.value)}
                  style={{ fontFamily: "'DM Mono',monospace" }}
                />
              </div>
              <button
                className="btn btn-green"
                style={{ width: '100%' }}
                onClick={handleSaveConfig}
                disabled={savingConfig}
                data-testid="save-config-btn"
              >
                {savingConfig
                  ? <><span className={styles.spinner} /> Guardando…</>
                  : <><Save size={14} /> Guardar cambios</>
                }
              </button>
            </>
          )}
        </div>

        {/* ── Prompts ───────────────────────────────────────────────────────── */}
        <div className={`${styles.card} ${styles.fullWidth}`} data-testid="prompts-section">
          <h3 className={styles.cardTitle}>✎ Plantillas de Prompts por Canal</h3>
          <div className={styles.promptTabs}>
            {PROMPT_TABS.map((t) => (
              <button
                key={t}
                className={`${styles.promptTab} ${promptTab === t ? styles.ptActive : ''}`}
                onClick={() => setPromptTab(t)}
              >
                {PROMPT_LABEL[t]}
              </button>
            ))}
          </div>
          {configLoading ? (
            <SkeletonBlock width="100%" height={140} />
          ) : (
            <textarea
              className={styles.promptEditor}
              value={prompts[promptTab]}
              onChange={(e) => setPrompts({ ...prompts, [promptTab]: e.target.value })}
            />
          )}
          <div className={styles.promptActions}>
            <button
              className="btn btn-green"
              onClick={handleSavePrompt}
              disabled={savingPrompt || configLoading}
              data-testid="save-prompt-btn"
            >
              {savingPrompt
                ? <><span className={styles.spinner} /> Guardando…</>
                : <><Save size={14} /> Guardar</>
              }
            </button>
          </div>
        </div>

        {/* ── Users ─────────────────────────────────────────────────────────── */}
        <div className={`${styles.card} ${styles.fullWidth}`} data-testid="users-section">
          <h3 className={styles.cardTitle}>👥 Gestión de Usuarios</h3>
          <table className={styles.usersTable}>
            <thead>
              <tr>
                {['Usuario', 'Email', 'Rol', 'Estado', 'Acciones'].map((h) => (
                  <th key={h} className={styles.uth}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {usersLoading ? (
                Array.from({ length: 3 }).map((_, i) => <SkeletonUserRow key={i} />)
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={5} className={styles.emptyCell}>No hay usuarios registrados</td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className={styles.urow}>
                    <td className={styles.utd}>
                      <div className={styles.userCell}>
                        <div className={styles.uavatar}>{getInitials(u.fullName)}</div>
                        {u.fullName}
                      </div>
                    </td>
                    <td className={styles.utdMono}>{u.email}</td>
                    <td className={styles.utd}>
                      <span className={`status-badge ${u.role === 'ADMIN' ? 'pending' : 'approved'}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className={styles.utd}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, fontFamily: "'DM Mono',monospace" }}>
                        <span style={{ width: 7, height: 7, borderRadius: '50%', background: u.active ? 'var(--green)' : 'var(--text3)', display: 'inline-block' }} />
                        {u.active ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className={styles.utd}>
                      <button
                        className="btn btn-ghost"
                        style={{ padding: '5px 12px', fontSize: 11 }}
                        onClick={() => setEditingUser(u)}
                        data-testid={`edit-user-btn-${u.id}`}
                      >
                        Editar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          <button
            className={styles.addBtn}
            style={{ marginTop: 16 }}
            onClick={() => setShowNewUser(true)}
            data-testid="new-user-btn"
          >
            <Plus size={14} /> Nuevo usuario
          </button>
        </div>

      </div>

      {/* ── Modals ─────────────────────────────────────────────────────────── */}
      {showNewUser && (
        <NewUserModal
          onClose={() => setShowNewUser(false)}
          onSave={handleCreateUser}
        />
      )}
      {editingUser && (
        <EditUserModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSave={handleUpdateUser}
        />
      )}
      {showNewSource && (
        <NewSourceModal
          onClose={() => setShowNewSource(false)}
          onSave={handleCreateSource}
        />
      )}
    </div>
  )
}
