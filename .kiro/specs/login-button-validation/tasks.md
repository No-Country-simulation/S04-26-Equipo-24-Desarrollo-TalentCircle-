# Implementation Plan: login-button-validation

## Overview

Implementación mínima en dos archivos: añadir la clase `.btnLoginDisabled` en `Login.module.css`, derivar `isFormValid` y actualizar el botón en `Login.jsx`. Luego instalar `fast-check` y escribir los tests de propiedad y de ejemplo en `Login.test.jsx`.

## Tasks

- [x] 1. Añadir la clase `.btnLoginDisabled` en `Login.module.css`
  - Añadir al final del archivo la regla `.btnLoginDisabled { background: var(--surface-2, #2a2f48); color: var(--text3, #6b7280); box-shadow: none; cursor: not-allowed; }`
  - La clase sobreescribe el gradiente amber de `.btnLogin` cuando se aplica conjuntamente, porque `background` tiene mayor especificidad que el `linear-gradient` heredado
  - _Requirements: 2.1, 2.2_

- [x] 2. Actualizar `Login.jsx` con la lógica de validación del formulario
  - [x] 2.1 Derivar la variable `isFormValid` a partir del estado existente
    - Añadir `const isFormValid = email.trim().length > 0 && password.trim().length > 0` justo antes del `return`
    - No introducir nuevo estado (`useState`); es una expresión derivada pura calculada en cada render
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - [x] 2.2 Actualizar el atributo `disabled` y el `className` del botón de submit
    - Cambiar `disabled={loading}` por `disabled={loading || !isFormValid}`
    - Cambiar `className={styles.btnLogin}` por `className={\`${styles.btnLogin} ${!isFormValid && !loading ? styles.btnLoginDisabled : ''}\`}`
    - La clase `.btnLoginDisabled` solo se aplica cuando `!isFormValid && !loading` (no durante la carga, para mantener el estilo amber con spinner)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.3, 3.1, 4.5_

- [x] 3. Checkpoint — Verificar la lógica antes de los tests
  - Revisar que `disabled={loading || !isFormValid}` cubre los tres estados de la tabla del diseño: campos vacíos sin carga (gris), campos llenos sin carga (amber), cargando (amber + spinner). Preguntar al usuario si hay dudas antes de continuar.

- [x] 4. Instalar `fast-check` como dependencia de desarrollo
  - Ejecutar `npm install --save-dev fast-check` en `frontend/talentcircle-app`
  - Verificar que `fast-check` aparece en `devDependencies` de `package.json`
  - _Requirements: (prerequisito de testing)_

- [x] 5. Escribir los tests en `Login.test.jsx`
  - [x] 5.1 Actualizar el test existente "shows loading spinner..." para reflejar el nuevo estado inicial
    - El test 1 existente asume `expect(btn).not.toBeDisabled()` antes del submit, lo cual ya no es válido: el botón arranca deshabilitado con campos vacíos
    - Rellenar los campos antes de hacer click en el botón, o ajustar la assertion inicial a `expect(btn).toBeDisabled()`
    - _Requirements: 1.1, 3.1, 3.2_
  - [x] 5.2 Escribir el test de propiedad para Property 1 — campos vacíos/solo-espacios deshabilitan el botón
    - **Property 1: Campos vacíos o solo-espacios deshabilitan el botón y aplican `.btnLoginDisabled`**
    - **Validates: Requirements 1.1, 2.1, 4.1, 4.2**
    - Usar `fc.string()` filtrado para que al menos uno de los dos campos tenga `trim().length === 0`
    - Assertion: `button.disabled === true` y `button.className` incluye `btnLoginDisabled`
    - Añadir el tag `// Feature: login-button-validation, Property 1: Campos vacíos o solo-espacios deshabilitan el botón`
    - Mínimo 100 iteraciones (`numRuns: 100`)
  - [x] 5.3 Escribir el test de propiedad para Property 2 — campos llenos habilitan el botón con estilo activo
    - **Property 2: Campos con contenido válido y sin carga habilitan el botón con estilo activo**
    - **Validates: Requirements 1.4, 2.3**
    - Usar `fc.string({ minLength: 1 })` filtrado para que ambos campos tengan `trim().length > 0`
    - Assertion: `button.disabled === false` y `button.className` NO incluye `btnLoginDisabled`
    - Añadir el tag `// Feature: login-button-validation, Property 2: Campos llenos habilitan el botón`
    - Mínimo 100 iteraciones (`numRuns: 100`)
  - [x] 5.4 Escribir el test de propiedad para Property 3 — `loading=true` deshabilita el botón independientemente de los campos
    - **Property 3: El estado de carga deshabilita el botón independientemente del contenido de los campos**
    - **Validates: Requirements 3.1**
    - Usar `fc.string()` para email y password (cualquier valor), con `loading = true` fijo (mock de `authApi.login` que nunca resuelve)
    - Assertion: `button.disabled === true`
    - Añadir el tag `// Feature: login-button-validation, Property 3: Loading deshabilita el botón`
    - Mínimo 100 iteraciones (`numRuns: 100`)
  - [x] 5.5 Escribir los 4 tests de ejemplo (example-based)
    - **Test A — Transición vacío → lleno:** escribir en ambos campos activa el botón (`disabled=false`, sin clase `btnLoginDisabled`) — _Requirements: 2.4_
    - **Test B — Transición lleno → vacío:** borrar un campo desactiva el botón (`disabled=true`, clase `btnLoginDisabled` presente) — _Requirements: 2.5_
    - **Test C — Spinner visible durante carga:** con campos llenos, al enviar el formulario el botón muestra el spinner y está deshabilitado — _Requirements: 3.2_
    - **Test D — Recuperación post-error:** tras `authApi.login` rechazar y `loading` volver a `false`, el botón se re-habilita si los campos siguen llenos — _Requirements: 3.3, 3.4_

- [x] 6. Checkpoint final — Asegurarse de que todos los tests pasan
  - Ejecutar `npx vitest run src/pages/Login/Login.test.jsx` en `frontend/talentcircle-app` y confirmar que todos los tests (existentes actualizados + nuevos) pasan sin errores. Preguntar al usuario si hay dudas antes de cerrar.

## Notes

- Las sub-tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- El orden importa: CSS primero (tarea 1), luego JSX (tarea 2), luego instalar dependencia (tarea 4), luego tests (tarea 5)
- `isFormValid` es una expresión derivada, no un nuevo `useState` — no añadir estado innecesario
- El test existente "shows loading spinner..." debe actualizarse (tarea 5.1): asume que el botón arranca habilitado, lo cual cambia con esta mejora
- El valor enviado a `authApi.login` sigue siendo sin `trim()`; la sanitización es responsabilidad del backend
- Los tests de propiedad usan `fast-check` con mínimo 100 iteraciones para cubrir edge cases (strings con tabs, newlines, combinaciones de espacios)
- Durante la carga (`loading=true`), el botón mantiene el estilo amber (sin `.btnLoginDisabled`) — el gris solo comunica "aún no puedes enviar esto"
