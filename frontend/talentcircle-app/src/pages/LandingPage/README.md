# TalentCircle Landing Page

<<<<<<< HEAD
> Página de aterrizaje oficial de TalentCircle — AI-powered content pipeline.

## 📁 Estructura

```
LandingPage/
├── LandingPage.jsx      ← Componente principal
├── LandingPage.css      ← Estilos globales de la landing
├── README.md            ← Este archivo
└── components/
    ├── Header/          ← Nav bar + logo + sign in
    ├── HeroSection/     ← Headline + CTA + stats
    ├── FeaturesSection/ ← 6 feature cards
    ├── HowItWorks/      ← 4-step pipeline explicación
    ├── NewsletterPreview/ ← Email signup form
    ├── CTASection/      ← Final CTA
    └── Footer/          ← Links + socials
```

## 🚀 Integración en el router

```jsx
import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'

const LandingPage = lazy(() => import('../pages/LandingPage/LandingPage'))
=======
A professional, modern, and fully responsive landing page for the TalentCircle editorial pipeline platform. This landing page serves as the public-facing introduction to TalentCircle, showcasing the platform's core capabilities and driving traffic to the application dashboard.

## 📋 Overview

The landing page is built with React 18, Vite, and modern CSS with a focus on:

- **Professional Design**: Modern gradient aesthetics aligned with TalentCircle's brand
- **Responsive Layout**: Fully responsive across all device sizes (mobile, tablet, desktop)
- **Performance**: Optimized for fast loading and smooth interactions
- **Accessibility**: Semantic HTML and proper navigation structure
- **User Engagement**: Strategic CTAs and newsletter subscription

## 🎨 Design System

### Color Palette

The landing page uses TalentCircle's established color system:

- **Primary**: Amber (`#f5a623`) - Action buttons and highlights
- **Secondary**: Teal (`#4ecdc4`) - Accents and gradients
- **Success**: Green (`#52d68a`) - Positive indicators
- **Warning**: Rose (`#ff6b8a`) - Alerts and warnings
- **Background**: Dark (`#0d0f14`) - Main background
- **Text**: Light (`#e8eaf2`) - Primary text

### Typography

- **Headlines**: Bold, uppercase letters with -0.01em letter-spacing
- **Body**: 0.95rem–1.125rem for readability
- **Code**: DM Mono for badges and status labels

### Spacing & Radius

- `--r`: 12px for standard border-radius
- `--r-sm`: 8px for smaller elements
- Consistent gap/padding system: 0.5rem → 4rem

## 🏗️ Component Structure

```
src/
├── pages/
│   └── LandingPage/
│       ├── LandingPage.jsx
│       └── LandingPage.css
├── components/
│   ├── Header/
│   │   ├── Header.jsx              ← Navigation bar with login button
│   │   └── Header.css
│   ├── HeroSection/
│   │   ├── HeroSection.jsx         ← Main headline & visual grid
│   │   └── HeroSection.css
│   ├── FeaturesSection/
│   │   ├── FeaturesSection.jsx     ← 6 feature cards
│   │   └── FeaturesSection.css
│   ├── HowItWorks/
│   │   ├── HowItWorks.jsx          ← 4-step pipeline visualization
│   │   └── HowItWorks.css
│   ├── NewsletterPreview/
│   │   ├── NewsletterPreview.jsx   ← Email signup + mockup
│   │   └── NewsletterPreview.css
│   ├── CTASection/
│   │   ├── CTASection.jsx          ← Final CTA with floating boxes
│   │   └── CTASection.css
│   └── Footer/
│       ├── Footer.jsx               ← Links & social
│       └── Footer.css
```

## 📄 Page Sections

### 1. **Header** (Sticky Navigation)

**Features:**
- Logo with pulsing icon animation
- Navigation links: "About Us", "Newsletter"
- Prominent "Login" button (redirects to `/talentcircle-app`)
- Mobile hamburger menu (768px breakpoint)
- Backdrop blur effect

**Key Elements:**
```jsx
<Header />
  ├─ Logo (clickable, links to home)
  ├─ Desktop Nav (About Us, Newsletter)
  ├─ Login Button (redirects to /talentcircle-app)
  └─ Mobile Menu (hidden on desktop)
```

### 2. **Hero Section**

**Features:**
- Compelling headline with gradient text
- Subheading with key value proposition
- Twin CTAs: "Get Started" and "Learn More"
- Statistics: Time saved, channels, real-time publishing
- Visual grid showcasing channel types (Newsletter, LinkedIn, Twitter/X)

**Key Elements:**
```jsx
<HeroSection />
  ├─ Badge (Intelligent Editorial Pipeline)
  ├─ Headline with gradient
  ├─ Description & CTAs
  ├─ Stats bar (80%, 3 Channels, Real-time)
  └─ Visual grid (3 cards)
```

### 3. **Features Section** (id: `#about`)

**Features:**
- 6-card grid showcasing core capabilities
- Icons via Lucide React
- Color-coded cards (amber, teal, green, rose)
- Hover animations (lift + glow effect)

**Features Highlighted:**
1. **Automated Pipeline** - Weekly collection and generation
2. **AI-Powered Analysis** - LLM integration for insights
3. **Multi-Channel Publishing** - Newsletter, LinkedIn, Twitter/X
4. **Community-First** - Built for tech communities
5. **Editorial Dashboard** - Review and approve interface
6. **Enterprise Security** - JWT, RBAC, audit logs

### 4. **How It Works**

**Features:**
- 4-step process visualization
- Icons and descriptions for each step
- Timeline showing execution schedule
- Responsive grid layout

**Steps:**
1. **Collect** - Gather community activities
2. **Analyze** - AI identifies valuable insights
3. **Generate** - Create optimized drafts
4. **Publish** - Review and publish content

**Timeline:**
- Every Friday automated execution
- 30 minutes for editor review
- Real-time publishing

### 5. **Newsletter Preview** (id: `#newsletter`)

**Features:**
- Email subscription form
- Input validation (email format)
- Success/error messages
- Email mockup showing Newsletter preview
- Feature checklist

**Newsletter Topics:**
- Weekly editorial automation tips
- Community content best practices
- Product updates and new features

### 6. **Call-to-Action Section**

**Features:**
- Final engagement section
- Primary CTA: "Login to Dashboard"
- Secondary CTA: "Learn More About Features"
- Support contact information
- Floating animated boxes (Analytics, Automated, AI-Powered)

### 7. **Footer**

**Features:**
- Brand information
- Navigation links (Product, Company, Legal)
- Social media links (Twitter, LinkedIn, GitHub, Email)
- Copyright and status indicator
- Responsive column layout

## 🚀 Integration with App Router

The landing page is designed to work alongside the existing app router. To integrate:

### Update `AppRouter.jsx`

```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import LandingPage from '../pages/LandingPage/LandingPage'
import Login from '../pages/Login/Login'
import Dashboard from '../pages/Dashboard/Dashboard'
>>>>>>> fff9a17a7c6a1b4f8f791db9ccc42ea2c62e8458

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
<<<<<<< HEAD
        <Route path="/" element={
          <Suspense fallback={<div>Loading...</div>}>
            <LandingPage />
          </Suspense>
        } />
        {/* otras rutas */}
=======
        <Route path="/" element={<LandingPage />} />
        <Route path="/talentcircle-app" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        {/* Other routes */}
>>>>>>> fff9a17a7c6a1b4f8f791db9ccc42ea2c62e8458
      </Routes>
    </BrowserRouter>
  )
}
```

<<<<<<< HEAD
## 🧩 Componentes

Cada componente vive en su propia carpeta con su CSS. No dependen de librerías externas.

| Componente | Props | Estado interno |
|------------|-------|----------------|
| Header | - | - |
| HeroSection | - | - |
| FeaturesSection | - | - |
| HowItWorks | - | - |
| NewsletterPreview | - | `email`, `status` (idle/success) |
| CTASection | - | - |
| Footer | - | - |

## 🎨 Estilos

- BEM naming: `.lp-header`, `.lp-hero__title`, etc.
- CSS personalizado sin frameworks UI
- Responsive por defecto (flexbox/grid)
- Variables de color inline (sin CSS custom properties)
=======
### Update `App.jsx`

```jsx
import { useEffect } from 'react'
import AppRouter from './router/AppRouter'
import Toast from './components/Toast'
import { useAppStore } from './store/useAppStore'

export default function App() {
  useEffect(() => {
    useAppStore.getState().initAuth()
  }, [])

  return (
    <>
      <AppRouter />
      <Toast />
    </>
  )
}
```

## 📱 Responsive Design

The landing page is optimized for all screen sizes with breakpoints at:

- **1280px** - Large tablets and small desktops
- **1024px** - Tablets
- **768px** - Mobile devices (header menu collapses)
- **480px** - Small mobile devices

**Mobile Optimizations:**
- Stack layout for multi-column sections
- Larger touch targets (buttons ≥ 44px)
- Adjusted font sizes for readability
- Hidden decorative elements on small screens
- Single-column grid layouts

## 🎯 Navigation & Smooth Scrolling

### Anchor Links

The landing page uses smooth scroll anchors:

- `#about` - Features section
- `#newsletter` - Newsletter subscription area

**Implementation:**
```jsx
const scrollToSection = (sectionId) => {
  const element = document.getElementById(sectionId)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
}
```

### Login Navigation

The login button redirects to `/talentcircle-app`:

```jsx
const handleLoginClick = () => navigate('/talentcircle-app')
```

## ✨ Animations

The landing page includes subtle, performance-optimized animations:

### CSS Animations
- **fadeUp** - Section entrance animations
- **fadeIn** - Content fade-in
- **pulse** - Logo icon and indicators
- **float** - Floating boxes in CTA section
- **spin** - Loading spinner

### Transitions
- `--transition: .2s cubic-bezier(.4,0,.2,1)` - Standard easing
- Hover effects on cards and buttons
- Transform animations on scroll

### Icons
- **Lucide React** v0.383+ for all icons
- Consistent sizing and styling
- Smooth icon animations

## 🔧 Configuration

### Environment Variables

The landing page doesn't require external env variables but respects the existing setup:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_NAME=TalentCircle
VITE_APP_VERSION=1.0.0
```

### Build Configuration

The Vite config should include the landing page route:

```js
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

## 🧪 Testing the Landing Page

### Local Development

```bash
cd frontend/talentcircle-app
npm install
npm run dev
```

Visit `http://localhost:5173` to see the landing page.

### Manual Testing Checklist

- [ ] Navigation links scroll smoothly
- [ ] Login button redirects to `/talentcircle-app`
- [ ] Newsletter form validates email
- [ ] Mobile menu opens/closes on small screens
- [ ] All sections are visible and responsive
- [ ] Images/icons load correctly
- [ ] Animations are smooth (60fps)
- [ ] Colors match brand guidelines
- [ ] Links to external resources work

## 📊 Performance Metrics

Target metrics:

- **Lighthouse Score** ≥ 90
- **First Contentful Paint** < 1s
- **Largest Contentful Paint** < 2.5s
- **Cumulative Layout Shift** < 0.1
- **Time to Interactive** < 3.5s

## 🎓 Best Practices

### Accessibility (A11y)

- Semantic HTML (`<section>`, `<nav>`, `<footer>`)
- Proper heading hierarchy (h1 → h2 → h3)
- ARIA labels where needed
- Color contrast ≥ 4.5:1
- Keyboard navigation support

### SEO

- Meta tags (title, description, og:image)
- Semantic heading structure
- Fast load times
- Mobile-responsive design
- Structured data markup (recommended)

### Code Quality

- Consistent naming conventions
- Component composition and reusability
- CSS organization (component-scoped styles)
- Responsive design mobile-first approach
- Performance optimizations

## 🚀 Deployment

### Build for Production

```bash
npm run build
```

Output: `dist/` folder

### Docker Build

The frontend Dockerfile handles building and serving:

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose

The landing page is served as part of the frontend service:

```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: talentcircle-app/Dockerfile
  ports:
    - "5173:80"
  depends_on:
    - backend
```

## 📝 File Structure

```
frontend/talentcircle-app/
├── src/
│   ├── pages/
│   │   └── LandingPage/
│   │       ├── LandingPage.jsx (182 lines)
│   │       └── LandingPage.css (25 lines)
│   ├── components/
│   │   ├── Header/ (89 lines JSX + 102 lines CSS)
│   │   ├── HeroSection/ (61 lines JSX + 166 lines CSS)
│   │   ├── FeaturesSection/ (62 lines JSX + 138 lines CSS)
│   │   ├── HowItWorks/ (86 lines JSX + 160 lines CSS)
│   │   ├── NewsletterPreview/ (70 lines JSX + 223 lines CSS)
│   │   ├── CTASection/ (70 lines JSX + 164 lines CSS)
│   │   └── Footer/ (97 lines JSX + 180 lines CSS)
│   ├── App.jsx
│   ├── index.css
│   └── main.jsx
└── package.json
```

## 🤝 Contributing

When extending the landing page:

1. Follow the existing component structure
2. Maintain responsive design across breakpoints
3. Use existing color variables from `index.css`
4. Keep animations performance-optimized
5. Add proper accessibility attributes
6. Test on multiple devices before submitting

## 📞 Support

For questions or issues with the landing page:

- Check the `README.md` in the main project
- Review the component-specific comments
- Test in different browsers (Chrome, Firefox, Safari, Edge)
- Use browser DevTools for responsive testing

---

**Built with ❤️ for TalentCircle Editorial Pipeline**

Version 1.0.0 | May 2026
>>>>>>> fff9a17a7c6a1b4f8f791db9ccc42ea2c62e8458
