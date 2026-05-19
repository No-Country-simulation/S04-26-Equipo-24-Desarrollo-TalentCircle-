import './FeaturesSection.css'

const features = [
  {
    icon: '📡',
    title: 'Community Collector',
    description: 'Pull activity from Discord, Circle, Slack, and more. Connect your communities and let the pipeline gather conversations automatically.',
  },
  {
    icon: '🤖',
    title: 'AI Analysis',
    description: 'LLM-powered analysis identifies trending topics, sentiment, and key insights from your community discussions.',
  },
  {
    icon: '✍️',
    title: 'Draft Generator',
    description: 'Generate newsletters, LinkedIn posts, and Twitter threads in Spanish from a single analysis. Ready for review.',
  },
  {
    icon: '📋',
    title: 'Editorial Panel',
    description: 'Review, edit, approve, or reject drafts before publishing. Full editorial control at your fingertips.',
  },
  {
    icon: '📤',
    title: 'Multi-Channel Publish',
    description: 'Schedule and publish to LinkedIn, Twitter, email newsletters, and more from one dashboard.',
  },
  {
    icon: '⚙️',
    title: 'Admin & Config',
    description: 'Configure LLM providers, manage API keys, set schedules, and monitor usage from the admin panel.',
  },
]

export default function FeaturesSection() {
  return (
    <section id="features" className="lp-features">
      <div className="lp-features__container">
        <h2 className="lp-features__title">Everything you need to scale your content</h2>
        <p className="lp-features__subtitle">
          From community conversations to published content in minutes.
        </p>
        <div className="lp-features__grid">
          {features.map((feature) => (
            <div key={feature.title} className="lp-features__card">
              <span className="lp-features__icon">{feature.icon}</span>
              <h3 className="lp-features__card-title">{feature.title}</h3>
              <p className="lp-features__card-desc">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
