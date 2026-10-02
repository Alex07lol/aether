# Aether GUI Reset — Glide-Style Rebuild

Status: implemented. This note records the visual grammar the reset copies from the
Glide Client 1.8.9 repository (`GlideClient/client`, commit `695c208`), what Aether
adopted, and what it deliberately implemented natively.

## The grammar (measured from Glide)

- One in-game screen, not one per section: a centered **450x280** window (GUI-scale
  pixels), corner radius 12, soft shadow (concentric strokes), dim backdrop.
- **32px icon rail** on the left: 22x22 logo chip, 21x21 nav items on a 30px pitch,
  an accent pill that glides between slots, an "Edit HUD" button at the rail bottom.
- Category content is clipped to the area right of the rail; category title 15px at
  the top; search 160x18 top-right; switching categories animates a 50px rise+fade;
  detail views (module settings) slide in horizontally.
- Module rows: 40 tall on a 50px step, radius 8; **toggle = the 28x28 icon chip**
  which fills with the accent when enabled; name 13px, description 9px inline; gear
  13px on the right. No visible scrollbar: the wheel scrolls an animated offset and
  12px edge fades signal more content.
- Settings: 2-column grid (column pitch 194, row 29); toggle 34x16 r7 with an
  11px knob; slider = 4px track + 8x8 thumb with a floating value; dropdown and
  keybind are 75x16 accent steppers, never popup lists; text fields 75x16 with a
  blinking caret; color picker = 16x16 swatch opening a 100x100 HSB popover.
- Cosmetics: chip tabs, 88x135 cards on a 100x147 grid (4 per row), selected =
  accent ring, thumbnail drawn rounded, name 10px centered.
- Themes: 36x36 swatches with a 1.4px accent outline on the active one.
- Profiles: 123x46 cards, 3 per row; one profile is one card; pagination on profiles.
- Type scale (Inter): 7 / 7.5 / 8 / 8.5 / 9 / 9.5 / 10 / 11 / 12.5 / 13 / 15.
- Surfaces: window = palette surface; cards/rows/inputs/rail = surface-soft; titles
  use the strong text colour, descriptions the soft one. Accent is a two-colour
  gradient fill on active states only — never a glow everywhere.
- Motion: exponential smoothing (speed 14-20), EaseBackIn 320ms intro, 260ms
  horizontal detail slide; all frame-rate independent.

## What Aether adopted vs. implemented natively

- **Adopted as numbers/behaviour**: every metric above, the one-screen + rail +
  routed-categories structure, the no-scrollbar scroll model, the component designs,
  the motion feel.
- **Implemented natively**: the vector renderer. Glide draws through NanoVG; Aether
  gets the same visual result (feathered rounded rects, per-corner radii, gradients,
  a rotating angular-gradient accent fill, soft shadows, scissored clipping) from a
  small GL11 immediate-mode layer in `dev.aether.ui.UiCanvas` that follows
  Mc189Compat's GL-state discipline. No NanoVG port, no rendering-stack migration.
- **Fonts**: Inter (SIL OFL) for UI text and Microsoft's Fluent System Icons (MIT)
  for chrome glyphs are bundled under `assets/aether/fonts/` with their license
  files; `AetherFontManager` rasterises them into the existing glyph-atlas pipeline.
  Glide's own `Icon.ttf`/`Gliconic.ttf` (custom, GPLv3-covered) are not copied.
- **Icons**: Fluent codepoints (MIT) selected in `dev.aether.ui.UiIcon`; a handful of
  constants take their codepoint values from Glide's MIT-licensed Fluent mapping.
- **Backend**: untouched, exactly as the brief requires - ModuleRegistry, Setting,
  HudLayout, ThemeManager, ClientPreferences, profiles, CosmeticLibrary +
  CosmeticMetadata, the cosmetic renderer, ModuleInputRouter (the menu reports
  keybind captures through `client.input()`), Anim/FrameClock.
- **Deleted with the reset**: `gui/leaf/*`, the old `ModuleRow`/`CosmeticCard`/
  `ChipBar`/`ScrollView`/`SearchBox` visual system, the six per-section screens and
  the design-space `GuiScale` layout they depended on. One GUI remains.
