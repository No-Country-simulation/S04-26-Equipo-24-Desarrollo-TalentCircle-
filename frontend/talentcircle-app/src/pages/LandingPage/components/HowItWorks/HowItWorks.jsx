import './HowItWorks.css'

const steps = [
  {
    number: '1',
    title: 'Connect Communities',
    description: 'Integrate with Discord, Slack, Circle, or any source. The collector pulls conversations automatically on schedule.',
  },
  {
    number: '2',
    title: 'AI Analyzes Activity',
    description: 'LLM identifies top topics, sentiment, and relevance scores from community discussions.',
  },
  {
    number: '3',
    title: 'Drafts Are Generated',
    description: 'Newsletter, LinkedIn, and Twitter drafts are created from the same analysis — each optimized for its channel.',
  },
  {
    number: '4',
    title: 'Review & Publish',
    description: 'Use the editorial panel to review, edit, and approve. Publish to all channels with one click.',
  },
]

export default function HowItWorks() {
  return (
    <section id="how-it-works" className="lp-howitworks">
      <div className="lp-howitworks__container">
        <h2 className="lp-howitworks__title">How the pipeline works</h2>
        <p className="lp-howitworks__subtitle">Four steps from community to content.</p>
        <div className="lp-howitworks__steps">
          {steps.map((step, index) => (
            <div key={step.number} className="lp-howitworks__step">
              <div className="lp-howitworks__step-number">{step.number}</div>
              <div className="lp-howitworks__step-content">
                <h3 className="lp-howitworks__step-title">{step.title}</h3>
                <p className="lp-howitworks__step-desc">{step.description}</p>
              </div>
              {index < steps.length - 1 && <div className="lp-howitworks__connector" />}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
