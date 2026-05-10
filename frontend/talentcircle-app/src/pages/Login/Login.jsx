import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppStore } from '../../store/useAppStore'
import authApi from '../../api/authApi'
import styles from './Login.module.css'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAppStore()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const response = await authApi.login(email, password)
      login(response)
      navigate('/dashboard')
    } catch {
      // Error toast is handled centrally by the apiClient interceptor
    } finally {
      setLoading(false)
    }
  }

  const isFormValid = email.trim().length > 0 && password.trim().length > 0

  return (
    <div className={styles.page}>
      <div className={styles.bg}>
        <div className={styles.grid} />
        <div className={styles.glow1} />
        <div className={styles.glow2} />
      </div>
      <form className={styles.card} onSubmit={handleSubmit}>
        <div className={styles.logoRow}>
          <div className={styles.logoIcon}>✦</div>
          <span className={styles.logoText}>Talent<em>Circle</em></span>
        </div>
        <h1 className={styles.heading}>Bienvenido de vuelta</h1>
        <p className={styles.sub}>Pipeline Inteligente de Contenido Comunitario</p>

        <div className="field">
          <label htmlFor="email">Correo electrónico</label>
          <input
            id="email"
            name="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="editor@talentcircle.com"
            autoComplete="email"
          />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            name="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="current-password"
          />
        </div>

        <button
          type="submit"
          className={`${styles.btnLogin} ${!isFormValid && !loading ? styles.btnLoginDisabled : ''}`}
          disabled={loading || !isFormValid}
        >
          {loading ? <span className={styles.spinner} /> : 'Ingresar al panel →'}
        </button>
      </form>
    </div>
  )
}
