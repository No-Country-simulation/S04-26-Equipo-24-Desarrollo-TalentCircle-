import './HeroSection.css'

export default function HeroSection() {
  return (
    <section className="lp-hero">
      <div className="lp-hero__container">
        <div className="lp-hero__content">
          <span className="lp-hero__badge">AI-Powered Content Pipeline</span>
          <h1 className="lp-hero__title">
            Transform community activity into<br />
            <span className="lp-hero__highlight">publish-ready content</span>
          </h1>
          <p className="lp-hero__subtitle">
            Automatically collect, analyze, and generate newsletters, LinkedIn posts,
            and Twitter threads from your community conversations.
          </p>
          <div className="lp-hero__actions">
            <a href="/login" className="lp-hero__btn lp-hero__btn--primary">
              Get Started Free
            </a>
            <a href="#how-it-works" className="lp-hero__btn lp-hero__btn--secondary">
              How It Works &rarr;
            </a>
          </div>
        </div>
        <div className="lp-hero__stats">
          <div className="lp-hero__stat">
            <span className="lp-hero__stat-number">10k+</span>
            <span className="lp-hero__stat-label">Drafts Generated</span>
          </div>
          <div className="lp-hero__stat">
            <span className="lp-hero__stat-number">500+</span>
            <span className="lp-hero__stat-label">Active Communities</span>
          </div>
          <div className="lp-hero__stat">
            <span className="lp-hero__stat-number">98%</span>
            <span className="lp-hero__stat-label">Satisfaction Rate</span>
          </div>
        </div>
      </div>
    </section>
  )
}
