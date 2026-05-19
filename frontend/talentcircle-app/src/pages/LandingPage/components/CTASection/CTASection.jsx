import './CTASection.css'

export default function CTASection() {
  return (
    <section className="lp-cta">
      <div className="lp-cta__container">
        <h2 className="lp-cta__title">
          Ready to automate your<br />community content?
        </h2>
        <p className="lp-cta__subtitle">
          Join hundreds of community managers saving hours every week.
        </p>
        <a href="/login" className="lp-cta__btn">Start Free &rarr;</a>
        <p className="lp-cta__footnote">No credit card required.</p>
      </div>
    </section>
  )
}
