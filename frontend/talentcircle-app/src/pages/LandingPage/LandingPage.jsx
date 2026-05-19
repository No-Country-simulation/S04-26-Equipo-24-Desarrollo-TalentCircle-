import './LandingPage.css'
import Header from './components/Header/Header'
import HeroSection from './components/HeroSection/HeroSection'
import FeaturesSection from './components/FeaturesSection/FeaturesSection'
import HowItWorks from './components/HowItWorks/HowItWorks'
import NewsletterPreview from './components/NewsletterPreview/NewsletterPreview'
import CTASection from './components/CTASection/CTASection'
import Footer from './components/Footer/Footer'

export default function LandingPage() {
  return (
    <div className="landing-page">
      <Header />
      <HeroSection />
      <FeaturesSection />
      <HowItWorks />
      <NewsletterPreview />
      <CTASection />
      <Footer />
    </div>
  )
}
