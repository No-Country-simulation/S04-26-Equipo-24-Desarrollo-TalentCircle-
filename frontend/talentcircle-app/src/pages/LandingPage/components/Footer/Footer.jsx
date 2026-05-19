import './Footer.css'

const footerLinks = [
  {
    heading: 'Product',
    links: ['Features', 'Pricing', 'Changelog', 'Documentation'],
  },
  {
    heading: 'Company',
    links: ['About', 'Blog', 'Careers', 'Contact'],
  },
  {
    heading: 'Legal',
    links: ['Privacy Policy', 'Terms of Service', 'Cookie Policy'],
  },
]

const socials = [
  { name: 'Twitter', href: '#' },
  { name: 'LinkedIn', href: '#' },
  { name: 'GitHub', href: '#' },
]

export default function Footer() {
  return (
    <footer className="lp-footer">
      <div className="lp-footer__container">
        <div className="lp-footer__brand">
          <span className="lp-footer__logo">✦ TalentCircle</span>
          <p className="lp-footer__desc">
            AI-powered content pipeline for community-driven teams.
          </p>
        </div>
        <div className="lp-footer__links">
          {footerLinks.map((group) => (
            <div key={group.heading} className="lp-footer__group">
              <h4 className="lp-footer__heading">{group.heading}</h4>
              <ul className="lp-footer__list">
                {group.links.map((link) => (
                  <li key={link}>
                    <a href="#" className="lp-footer__link">{link}</a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
      <div className="lp-footer__bottom">
        <p className="lp-footer__copyright">&copy; {new Date().getFullYear()} TalentCircle. All rights reserved.</p>
        <div className="lp-footer__socials">
          {socials.map((social) => (
            <a key={social.name} href={social.href} className="lp-footer__social">{social.name}</a>
          ))}
        </div>
      </div>
    </footer>
  )
}
