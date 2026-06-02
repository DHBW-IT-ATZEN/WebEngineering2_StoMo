# Design System Specification: The Architectural Ledger

## 1. Overview & Creative North Star
This design system is built upon the **Creative North Star: The Architectural Ledger.** 

Financial dashboards often suffer from "data-clutter"—a sea of borders, boxes, and competing grids. This system rejects the generic "SaaS template" look in favor of a high-end editorial experience. We treat financial data with the same reverence as a luxury architectural monograph. By utilizing intentional asymmetry, deep tonal layering, and sophisticated typography, we move away from "software" and toward a "curated intelligence tool."

The system is designed to feel anchored and authoritative. We replace structural rigidity with fluid depth, using light and shadow rather than lines to define the space.

---

## 2. Colors & Tonal Depth

### The "No-Line" Rule
To achieve a premium, custom feel, **1px solid borders are prohibited for sectioning.** Boundaries must be defined solely through background color shifts or tonal transitions. Use `surface_container_low` against a `surface` background to create a section. This forces a cleaner, more organic layout that feels "carved" rather than "assembled."

### Surface Hierarchy & Nesting
Instead of a flat grid, treat the UI as a series of physical layers. We use the surface-container tiers to define importance:
- **`surface_container_lowest`**: Used for the deep background or "canvas."
- **`surface_container` / `high`**: Used for primary content modules.
- **`surface_container_highest`**: Used for interactive overlays or high-priority callouts.

### The "Glass & Gradient" Rule
Floating elements (modals, dropdowns, or active state overlays) should utilize **Glassmorphism**. Apply a semi-transparent `surface_variant` with a 12px–20px backdrop blur. 
For primary CTAs, use a subtle linear gradient transitioning from `primary` to `primary_container` (e.g., 135-degree angle). This adds a "visual soul" and a sense of physical material that flat turquoise cannot achieve.

### The Themes
1.  **Default Theme (Dark Mode):** Utilizes the `background` (`#0b1326`) with `primary` turquoise (`#47eaed`) accents. It is designed for high-focus, low-glare financial analysis.
2.  **Yoda Mode (The Earth Shift):** When the toggle is engaged, the palette shifts to linen/earth tones. The `surface` becomes a warm, desaturated linen, while the `primary` role is inherited by a rich forest green (mapping to the `on_secondary_fixed_variant` and `on_primary_container` tokens in this context).

---

## 3. Typography: Editorial Authority
The typography scale is designed to balance the brutalist geometry of financial data with the elegance of high-end publishing.

*   **Display & Headlines (Manrope):** Chosen for its geometric precision. Use `display-lg` and `headline-md` to create "Editorial Moments"—large, unapologetic typographic anchors that break the grid.
*   **Body & Labels (Inter):** A workhorse for financial clarity. Inter’s tall x-height ensures that complex numbers remain legible at `body-sm` (0.75rem).
*   **The "Data-Ink" Philosophy:** Use `label-md` for metadata. Numbers should never feel "crowded." Increase letter-spacing (tracking) by 1-2% for all uppercase labels to enhance the premium feel.

---

## 4. Elevation & Depth
We achieve hierarchy through **Tonal Layering** and **Ambient Light** rather than traditional structural lines.

*   **The Layering Principle:** Place a `surface_container_lowest` card on a `surface_container_low` section to create a soft, natural lift. This mimics the way matte paper layers on top of a desk.
*   **Ambient Shadows:** For floating components (e.g., the Theme Toggle or Tooltips), use extra-diffused shadows:
    *   **Blur:** 24px–40px.
    *   **Opacity:** 4%–8%.
    *   **Color:** Use a tinted version of `on_surface` (deep navy or forest green) to mimic natural light reflection.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility, it must be a **Ghost Border**: use `outline_variant` at 15% opacity. Never use 100% opaque lines.

---

## 5. Components

### The Mode Toggle (Header)
A bespoke switch located in the top-right utility area.
- **Track:** `surface_container_highest` with a `full` roundedness scale.
- **Thumb:** A glassmorphic circle (`surface_bright` with backdrop blur).
- **Icons:** Use a subtle "Moon" icon for Default and a "Leaf" icon for Yoda Mode. The active icon should take the `primary` color.

### Primary Buttons
- **Shape:** `md` (0.375rem) roundedness for a balanced, professional look.
- **Fill:** Gradient from `primary` to `primary_container`.
- **Text:** `label-md` in `on_primary_fixed`, set in all-caps with +3% tracking for an authoritative "button" feel.

### Financial Data Cards
- **No Dividers:** Forbid the use of divider lines between rows. Use vertical white space (from the spacing scale) or a subtle shift to `surface_container_low` for alternating rows.
- **Contextual Glass:** Use a glassmorphic background for "Total Balance" or "Real-time Profit" cards to make them feel like they are floating above the data landscape.

### Input Fields
- **State:** `outline` for resting, `primary` (Turquoise/Forest Green) for focus.
- **Background:** `surface_container_lowest` to create a "recessed" feel, as if the input is carved into the interface.

---

## 6. Do's and Don'ts

### Do:
- **Use Asymmetry:** Place a large `display-md` balance on the left with a significantly smaller `label-sm` trend indicator to create a dynamic visual path.
- **Embrace Negative Space:** Allow data points to "breathe." Financial dashboards are often too tight; this system requires generous padding.
- **Tone-on-Tone:** Use `on_surface_variant` for secondary text to keep the visual hierarchy soft and sophisticated.

### Don't:
- **Don't use 1px Borders:** This is the most common mistake. If you feel the need for a line, try a 4px gap of the background color instead.
- **Don't use Pure Black Shadows:** Shadows must be tinted to the theme's dark-blue or earth-green palette to maintain the "Architectural" feel.
- **Don't over-use the Accent:** The dark turquoise and forest green are "surgical" colors. Use them for active states, CTAs, and critical data points—never for large background fills.