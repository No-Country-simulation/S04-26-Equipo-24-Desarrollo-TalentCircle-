import { useState, useEffect } from 'react'
import './NewsletterPreview.css'

const MOCK_POSTS = [
  {
    id: '1',
    title: 'Weekly Community Digest: Spring Boot & Architecture Trends',
    date: 'May 18, 2026',
    excerpt: 'This week our community explored microservices patterns, with heated discussions on hexagonal architecture vs layered architecture. Key takeaway: 73% of members prefer hexagonal for new projects.',
  },
  {
    id: '2',
    title: 'Top 5 Discussions: AI Integration in Java Apps',
    date: 'May 11, 2026',
    excerpt: 'Members shared real-world experiences integrating LLMs into Java backends. The most liked approach? A modular LLM client port with automatic provider fallback — now part of our open-source pipeline.',
  },
  {
    id: '3',
    title: 'Community Spotlight: Open Source Contributions',
    date: 'May 4, 2026',
    excerpt: 'Three new contributors joined the TalentCircle project this week. Highlights include a Discord collector module, improved error handling, and a new SSE streaming endpoint for real-time draft generation.',
  },
]

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export default function NewsletterPreview() {
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    if (!BASE_URL) {
      setPosts(MOCK_POSTS)
      setLoading(false)
      return
    }

    fetch(`${BASE_URL}/api/v1/public/newsletters`, { signal: AbortSignal.timeout(5000) })
      .then((res) => {
        if (!res.ok) throw new Error('Failed to fetch')
        return res.json()
      })
      .then((data) => {
        if (data && data.length > 0) {
          setPosts(
            data.map((n) => ({
              id: n.id,
              title: n.title,
              date: formatDate(n.date),
              excerpt: n.excerpt,
            }))
          )
        } else {
          setPosts(MOCK_POSTS)
        }
        setLoading(false)
      })
      .catch(() => {
        setError(true)
        setPosts(MOCK_POSTS)
        setLoading(false)
      })
  }, [])

  function formatDate(dateStr) {
    if (!dateStr) return ''
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
  }

  return (
    <section id="newsletter" className="lp-newsletter">
      <div className="lp-newsletter__container">
        <div className="lp-newsletter__header">
          <span className="lp-newsletter__badge">Newsletter</span>
          <h2 className="lp-newsletter__title">What TalentCircle generates</h2>
          <p className="lp-newsletter__subtitle">
            Real newsletter drafts automatically created from community conversations.
            Each edition is AI-generated and ready for editorial review.
          </p>
        </div>

        <div className="lp-newsletter__feed">
          {loading ? (
            <div className="lp-newsletter__loading">Loading latest newsletters...</div>
          ) : (
            posts.map((post) => (
              <article key={post.id} className="lp-newsletter__post">
                <div className="lp-newsletter__post-body">
                  <div className="lp-newsletter__post-meta">
                    <span className="lp-newsletter__post-date">{post.date}</span>
                  </div>
                  <h3 className="lp-newsletter__post-title">{post.title}</h3>
                  <p className="lp-newsletter__post-excerpt">{post.excerpt}</p>
                </div>
              </article>
            ))
          )}
        </div>

        <div className="lp-newsletter__subscribe">
          <p className="lp-newsletter__subscribe-text">
            Want these newsletters delivered to your inbox?
          </p>
          <form className="lp-newsletter__form">
            <input
              type="email"
              placeholder="Enter your email"
              className="lp-newsletter__input"
              required
            />
            <button type="submit" className="lp-newsletter__btn">
              Subscribe
            </button>
          </form>
        </div>
      </div>
    </section>
  )
}
