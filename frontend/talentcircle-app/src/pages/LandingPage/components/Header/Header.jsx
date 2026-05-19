import './Header.css'

const navigation = [
  { name: 'Features', href: '#features' },
  { name: 'How It Works', href: '#how-it-works' },
  { name: 'Newsletter', href: '#newsletter' },
]

export default function Header() {
  return (
    <header className="lp-header">
      <nav className="lp-header__nav">
        <div className="lp-header__logo">
          <span className="lp-header__logo-icon">✦</span>
          <span className="lp-header__logo-text">Talent<em>Circle</em></span>
        </div>
        <ul className="lp-header__links">
          {navigation.map((item) => (
            <li key={item.name}>
              <a href={item.href} className="lp-header__link">{item.name}</a>
            </li>
          ))}
        </ul>
        <a href="/login" className="lp-header__cta">Sign In</a>
      </nav>
    </header>
  )
}
