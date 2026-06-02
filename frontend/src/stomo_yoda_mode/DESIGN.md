---
name: Yoda Mode
colors:
  surface: '#f9faf2'
  surface-dim: '#d9dbd3'
  surface-bright: '#f9faf2'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4ed'
  surface-container: '#edefe7'
  surface-container-high: '#e7e9e1'
  surface-container-highest: '#e2e3dc'
  on-surface: '#191c18'
  on-surface-variant: '#42493e'
  inverse-surface: '#2e312c'
  inverse-on-surface: '#f0f1ea'
  outline: '#72796e'
  outline-variant: '#c2c9bb'
  surface-tint: '#3b6934'
  primary: '#154212'
  on-primary: '#ffffff'
  primary-container: '#2d5a27'
  on-primary-container: '#9dd090'
  inverse-primary: '#a1d494'
  secondary: '#5c613e'
  on-secondary: '#ffffff'
  secondary-container: '#dee4b7'
  on-secondary-container: '#606642'
  tertiary: '#60233e'
  on-tertiary: '#ffffff'
  tertiary-container: '#7c3a55'
  on-tertiary-container: '#ffaac8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bcf0ae'
  primary-fixed-dim: '#a1d494'
  on-primary-fixed: '#002201'
  on-primary-fixed-variant: '#23501e'
  secondary-fixed: '#e0e6ba'
  secondary-fixed-dim: '#c4ca9f'
  on-secondary-fixed: '#191e03'
  on-secondary-fixed-variant: '#444a29'
  tertiary-fixed: '#ffd9e4'
  tertiary-fixed-dim: '#ffb0cc'
  on-tertiary-fixed: '#3b0520'
  on-tertiary-fixed-variant: '#71314c'
  background: '#f9faf2'
  on-background: '#191c18'
  surface-variant: '#e2e3dc'
typography:
  display:
    fontFamily: Manrope
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-max: 1280px
  gutter: 24px
  margin-desktop: 64px
  margin-mobile: 20px
---

## Brand & Style

This design system is defined by an "Organic Architectural" aesthetic. It merges the rigid, geometric precision of modern architecture with the warmth of natural, earthy materials. The goal is to create a digital environment that feels like a sunlit studio—serene, organized, and breathable. 

The brand personality is authoritative yet calm, moving away from the cold sterility of traditional tech interfaces toward a more tactile, human-centric experience. It avoids aggressive visual cues in favor of subtle depth and high-quality typography, ensuring that the interface recedes to let the content remain the primary focus.

## Colors

The palette is anchored by a linen base (`#F5F5DC`), providing a warm, low-strain canvas that feels more premium than pure white. The primary accent is a rich, forest-grade saturated green, used purposefully for calls to action and critical brand moments.

To maintain high readability, text uses a "Dark Walnut" off-black rather than true black, softening the contrast while maintaining accessibility. Secondary accents utilize muted olive and sage tones to create a cohesive, monochromatic harmony that reinforces the organic theme.

## Typography

This design system exclusively employs **Manrope**, leveraging its unique balance between geometric shapes and humanist proportions. 

Headlines should be set with tighter tracking to emphasize their architectural structure, while body copy is given ample line height to ensure maximum legibility against the linen background. For display sizes, a heavier weight is preferred to create a clear visual hierarchy. Labels and small utility text should use semi-bold or bold weights with slight letter-spacing to maintain clarity at smaller scales.

## Layout & Spacing

The layout philosophy follows a strict 8px rhythm, ensuring mathematical harmony across all components. A 12-column fluid grid is used for desktop layouts, transitioning to a 4-column grid for mobile devices.

Whitespace is treated as a first-class design element—generous margins and internal padding are used to prevent the "boxy" feel of traditional UI. Elements should breathe; avoid crowding components. Use the `container-max` to ensure line lengths remain comfortable on wide displays, centering the content to create a focused, editorial feel.

## Elevation & Depth

Depth in this design system is achieved through "Ambient Tactility." Instead of heavy borders or dark containers, use soft, multi-layered shadows that simulate a gentle lift off the linen surface. 

Shadows should have a slight warm tint (using a dark umber or olive rather than pure gray) to stay integrated with the background. Surfaces are distinguished by subtle tonal shifts—moving from the base linen to a slightly lighter "Paper" surface for elevated cards. Soft, low-opacity borders (1px width, 10-15% opacity) may be used to define boundaries in high-density areas without adding visual noise.

## Shapes

The shape language is "Soft-Geometric." A standard radius of `0.5rem` (8px) is applied to primary UI elements like cards and input fields, providing a friendly but professional appearance. Larger components like hero sections or modals utilize `1.5rem` to emphasize the "organic" nature of the design. 

Avoid sharp 0px corners, as they conflict with the natural aesthetic. Conversely, avoid full "pill" shapes for standard buttons to maintain the architectural structure of the Manrope typeface; reserve pill shapes strictly for tags and status chips.

## Components

### Buttons
Primary buttons feature the rich green accent with white or high-contrast cream text. They should use a subtle inner-glow rather than a harsh drop shadow to appear "pressed" into the surface when active. Secondary buttons use a ghost style with a soft-olive border.

### Cards
Cards are the primary container. They should not have heavy borders; instead, use the lighter surface color and a diffuse ambient shadow. Padding within cards should be generous (minimum 24px).

### Input Fields
Inputs should feel integrated. Use a soft, bone-colored background with a 1px stroke that darkens only on focus. The focus state should utilize a subtle 3px outer glow in the primary green at low opacity.

### Chips & Tags
Use pill-shaped containers with a slightly more saturated version of the background color (e.g., a soft sage). Text should be uppercase `label-sm` for a refined, metadata-heavy appearance.

### Lists & Navigation
Navigation items should use `label-md` with ample horizontal spacing. Active states are indicated by a small, organic dot below the text or a subtle shift in font weight, avoiding heavy underlines.