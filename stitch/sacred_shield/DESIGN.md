# Design System Specification: The Ethereal Guardian

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Ethereal Guardian."** 

This system moves beyond the utility of a standard content blocker to create a digital sanctuary. We are blending the mathematical precision of Islamic geometry with the cutting-edge aesthetic of high-end "Cyber-Spiritualism." The experience must feel protective yet expansive.

To break the "template" look, we employ **intentional asymmetry** and **tonal depth**. Rather than a standard vertical stack of cards, we use generous whitespace (using the `20` and `24` spacing tokens) to allow elements to breathe, creating an editorial feel. Elements should occasionally overlap—such as a floating action card partially obscuring a background geometric pattern—to create a sense of three-dimensional space.

## 2. Colors & Surface Philosophy
The palette is rooted in deep, obsidian blacks and protective teals. 

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections. Boundaries must be defined solely through background color shifts. 
- A card (`surface-container-low`) should sit on a `surface` background. 
- An inner action area should use `surface-container-high` to distinguish itself. 
- Structural integrity is achieved through contrast, not lines.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. 
- **Base Layer:** `surface` (#0e0e0e) or `surface-container-lowest` (#000000).
- **Secondary Layer:** `surface-container` (#191919) for large content areas.
- **Interactive Layer:** `surface-container-highest` (#262626) for the most prominent cards.

### The Glass & Gradient Rule
To move beyond a "flat" feel, floating elements (like navigation bars or modal overlays) must utilize **Glassmorphism**. Use a semi-transparent version of `surface-variant` with a `backdrop-blur` of 20px–40px. 
**Signature Textures:** Main CTAs should not be flat. Apply a subtle linear gradient from `primary` (#61f4d8) to `primary-container` (#08c1a6) at a 135-degree angle to provide a "glowing" soul to the interface.

## 3. Typography
We use a high-contrast typographic pairing to balance modern tech with editorial authority.

*   **Display & Headlines (Manrope):** This is our "Authoritative" voice. Use `display-lg` for hero moments with tight letter-spacing (-0.02em). The geometric nature of Manrope mirrors the Islamic patterns in the background.
*   **Body & Titles (Plus Jakarta Sans):** This is our "Human" voice. It is highly legible and friendly. Use `body-lg` for primary content to maintain a premium, spacious feel.
*   **Scale as Hierarchy:** Embrace the extremes. Use a massive `display-md` headline next to a tiny, all-caps `label-sm` in `on-surface-variant` to create a sophisticated, high-end editorial rhythm.

## 4. Elevation & Depth
In this system, light comes from within the components, not from an external source.

*   **Tonal Layering:** Depth is achieved by "stacking." A `surface-container-low` section should house `surface-container-highest` cards. This creates a soft, natural lift without the clutter of drop shadows.
*   **Ambient Shadows:** If a "floating" effect is required (e.g., a bottom sheet), use a shadow with a 64px blur, 0px offset, and 6% opacity using the `primary` color (#61f4d8) instead of black. This creates a "teal glow" that feels spiritual and high-tech.
*   **The "Ghost Border" Fallback:** If a container lacks sufficient contrast against its background, use a "Ghost Border": the `outline-variant` token at 15% opacity. Never use 100% opacity for strokes.
*   **Geometric Patterns:** Subtle Islamic patterns should be masked within `surface-container-low` layers at 3-5% opacity. They should feel like a watermark—visible only upon close inspection.

## 5. Components

### Cards & Lists
- **Style:** Minimum radius of `xl` (3rem/48px) for primary containers.
- **Constraint:** Forbid all divider lines. Use `spacing-6` (2rem) as a vertical gutter between list items or subtle background shifts (`surface-container-low` vs `surface-container-high`).

### Buttons
- **Primary:** Gradient from `primary` to `primary-container`. Text color: `on-primary-container`. Shape: `full` (pill).
- **Secondary:** Transparent background with a "Ghost Border" (15% `outline`).
- **Interaction:** On hover/press, the `primary` glow should expand using an inner shadow of `primary_dim`.

### Input Fields
- **Style:** Use `surface-container-highest` for the field background. 
- **Focus State:** No border change. Instead, the background should transition to `surface-bright` and the label should shift to the `primary` teal color.

### The "Veil" Toggle (System-Specific)
A bespoke component for this system. A large, circular `xl` rounded card featuring a centered `shield` icon. When active, the card should emit a `primary_fixed` outer glow and the background Islamic pattern should increase in opacity from 3% to 8%.

### Chips
- Use `secondary-container` for active states.
- Roundedness: `md` (1.5rem) to maintain the "soft-tech" feel.

## 6. Do's and Don'ts

### Do
- **Do** use asymmetrical layouts (e.g., a large headline on the left with a small caption tucked into the bottom right of a card).
- **Do** use the `primary` teal sparingly as a "light source" to guide the user's eye to the most important action.
- **Do** ensure all text on `background` is at least `on-background` (#ffffff) for maximum spiritual clarity and accessibility.

### Don't
- **Don't** use 90-degree corners. Everything must feel smoothed and "Protective."
- **Don't** use standard Material 3 "elevated" shadows. They are too heavy and "dirty" for this ethereal aesthetic.
- **Don't** overcrowd the screen. If a screen feels full, increase the spacing to `spacing-10` or `12`. Silence is as important as sound; whitespace is as important as content.