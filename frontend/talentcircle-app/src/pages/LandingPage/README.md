# TalentCircle Landing Page

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

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={
          <Suspense fallback={<div>Loading...</div>}>
            <LandingPage />
          </Suspense>
        } />
        {/* otras rutas */}
      </Routes>
    </BrowserRouter>
  )
}
```

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
