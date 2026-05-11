# Requirements Document

## Introduction

Esta mejora añade validación visual al botón "Ingresar al panel" del formulario de login de TalentCircle. Actualmente el botón solo se deshabilita durante el estado de carga (`loading`). La mejora extiende ese comportamiento para que el botón también esté deshabilitado —y visualmente apagado— cuando los campos de correo electrónico o contraseña estén vacíos o contengan únicamente espacios en blanco. Cuando ambos campos tienen contenido válido, el botón recupera su estilo amber activo y permite enviar el formulario.

## Glossary

- **Login_Button**: El elemento `<button type="submit">` con clase `.btnLogin` dentro del formulario de login.
- **Email_Field**: El campo `<input type="email" id="email">` controlado por el estado `email` en `Login.jsx`.
- **Password_Field**: El campo `<input type="password" id="password">` controlado por el estado `password` en `Login.jsx`.
- **Fields_Empty_State**: Condición en la que al menos uno de los campos (`email` o `password`) está vacío o contiene únicamente caracteres de espacio en blanco.
- **Fields_Filled_State**: Condición en la que tanto `email` como `password` contienen al menos un carácter no-espacio en blanco.
- **Loading_State**: Condición en la que la variable de estado `loading` es `true`, indicando que una petición de autenticación está en curso.
- **Active_Style**: Estilo visual amber/dorado definido por `.btnLogin` (gradiente `var(--amber)` → `#e8830a`).
- **Disabled_Style**: Estilo visual apagado que indica que el botón no es interactuable (color gris/neutro, sin gradiente amber).

---

## Requirements

### Requirement 1: Deshabilitar el botón cuando los campos están incompletos

**User Story:** Como usuario del panel de TalentCircle, quiero que el botón "Ingresar al panel" esté deshabilitado cuando no he completado ambos campos, para que no pueda enviar el formulario accidentalmente con datos incompletos.

#### Acceptance Criteria

1. WHILE `Fields_Empty_State` is active, THE `Login_Button` SHALL have the `disabled` attribute set.
2. WHEN the `Email_Field` value changes to contain only whitespace characters, THE `Login_Button` SHALL be disabled immediately.
3. WHEN the `Password_Field` value changes to contain only whitespace characters, THE `Login_Button` SHALL be disabled immediately.
4. WHILE `Fields_Filled_State` is active AND `Loading_State` is inactive, THE `Login_Button` SHALL NOT have the `disabled` attribute set.
5. IF the user attempts to submit the form while `Fields_Empty_State` is active, THEN THE `Login_Button` SHALL prevent form submission.

---

### Requirement 2: Estilo visual diferenciado para el estado deshabilitado por campos vacíos

**User Story:** Como usuario del panel de TalentCircle, quiero que el botón deshabilitado por campos vacíos tenga un aspecto visualmente apagado, para que pueda distinguir claramente cuándo el botón está activo y cuándo no.

#### Acceptance Criteria

1. WHILE `Fields_Empty_State` is active, THE `Login_Button` SHALL display a muted/grey visual style that is visually distinct from the `Active_Style`.
2. WHILE `Fields_Empty_State` is active, THE `Login_Button` SHALL NOT display the amber gradient defined in `Active_Style`.
3. WHILE `Fields_Filled_State` is active AND `Loading_State` is inactive, THE `Login_Button` SHALL display the `Active_Style` (amber gradient).
4. WHEN the user transitions from `Fields_Empty_State` to `Fields_Filled_State` by typing in the last empty field, THE `Login_Button` SHALL update its visual style to `Active_Style` without requiring a page reload.
5. WHEN the user transitions from `Fields_Filled_State` to `Fields_Empty_State` by clearing a field, THE `Login_Button` SHALL update its visual style to `Disabled_Style` without requiring a page reload.

---

### Requirement 3: Compatibilidad con el estado de carga existente

**User Story:** Como usuario del panel de TalentCircle, quiero que el comportamiento de carga del botón siga funcionando igual que antes, para que la experiencia durante el envío del formulario no se vea afectada.

#### Acceptance Criteria

1. WHILE `Loading_State` is active, THE `Login_Button` SHALL have the `disabled` attribute set, regardless of the content of `Email_Field` and `Password_Field`.
2. WHILE `Loading_State` is active, THE `Login_Button` SHALL display the loading spinner in place of the button text.
3. WHEN `Loading_State` transitions to inactive after a failed authentication attempt, THE `Login_Button` SHALL re-evaluate `Fields_Empty_State` and `Fields_Filled_State` to determine the correct enabled/disabled state.
4. WHEN `Loading_State` transitions to inactive after a failed authentication attempt AND `Fields_Filled_State` is active, THE `Login_Button` SHALL return to the `Active_Style` and be enabled.

---

### Requirement 4: Evaluación de campos basada en contenido no-vacío

**User Story:** Como usuario del panel de TalentCircle, quiero que el sistema considere un campo como "vacío" si solo contiene espacios en blanco, para que no pueda enviar credenciales que son efectivamente vacías.

#### Acceptance Criteria

1. THE `Login_Button` SHALL evaluate `Email_Field` as empty when its trimmed value has a length of 0 characters.
2. THE `Login_Button` SHALL evaluate `Password_Field` as empty when its trimmed value has a length of 0 characters.
3. THE `Login_Button` SHALL evaluate `Email_Field` as filled when its trimmed value has a length greater than 0 characters.
4. THE `Login_Button` SHALL evaluate `Password_Field` as filled when its trimmed value has a length greater than 0 characters.
5. WHEN `Email_Field` contains only space characters AND `Password_Field` contains valid content, THE `Login_Button` SHALL remain disabled.
