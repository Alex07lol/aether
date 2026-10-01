# Aether In-Game GUI — the Leaf Client port

Status: implemented and building green. This document records how Aether's in-game screens came to
be a copy of Leaf Client 1.8.9's screens, what is deliberately still Aether's own, and how the copy
is verified. It is written as a decision record, not a plan.

References:

- Leaf Client (visual baseline): https://github.com/Lefiy/Leaf-Client — `leafclient-1.8.9`, read
  from a local clone; GPLv3, same licence as Aether, so the port and the derived art are permitted
  with attribution.
- Aether core (`dev.aether.*`) is unchanged in spirit: the GUI remains a thin consumer of
  `ModuleRegistry`, `Setting`, `CosmeticLibrary`, `HudLayout`, `ClientPreferences` and
  `ProfileStore`. No screen owns state.

---

## 1. What "copy Leaf exactly" means here

The first Aether rebuild took Leaf's *structure* (one screen per section, a nav row, a card grid)
and drew everything procedurally in Aether's purple-on-charcoal palette; the original brief then
changed twice, and the second version was explicit: **copy Leaf's UI exactly, in transparent black
and white.** The second person's-eye version of that is:

1. **The art is Leaf's.** Every control is drawn from the original Leaf texture, with the shape and
   the panel detail preserved 1:1 and only the colours replaced.
2. **The geometry is Leaf's.** Same design coordinates, same sizes, same interaction split (a card's
   top half toggles, its bottom half opens settings), same scrollbar behaviour.
3. **The palette is black and white.** Translucent black surfaces, white detail, no second hue -
   so the game stays visible through the UI.

### The art pipeline (`scripts/leaf_assets.py`)

The recolouring is a build-time step with Pillow; the mod only ever loads the results, so there is no
image processing and no `ImageIO` in the shipped client.

Measuring the source art first was necessary, because it is not photographic: nearly every texture
uses two or three **flat luminance tones** and two or three **flat alpha plateaus**.

| alpha | art |
| --- | --- |
| 128 | every panel: cards, tabs, square buttons, selects, sliders, scrollbars, text fields |
| 255 | pure glyphs: `gear_small`, `next`, `back`, `play`, `stop`, `hide`, `show`, `leaf` |
| 192 | the toggles' knobs, on a 128 plate |
| 38 / 142 | the backdrops: a fullscreen scrim with a rounded panel sitting on it |

The **shadow is the trap**: it is a ramp *below* the plateau and carries the *same* dark tone as the
icons, so luminance cannot tell a shadow from a glyph. The plateau can, which is what the script
keys on:

- `surface_mask(image)` — `smoothstep(plateau*0.80, plateau*0.95, alpha)`: 1 inside the shape, 0 in
  the shadow ramp.
- `body_luminance(image, mask)` — the mode of the luminance over the fully-interior pixels, i.e. the
  panel's own tone, measured per file instead of hand-tuned. This handles both polarities: Leaf's
  tabs and cards are light bodies with dark glyphs, its arrow buttons are the reverse.

Rules: `glass(strength)`, `white(strength)`, `shade(strength, lo, hi)`, `detail(strength, lo, hi)`
and `detail(strength, lo, hi, alpha_gain)`. `detail` composes a black-glass body with the texture's
own detail (a luminance difference *inside* the surface) keyed to white; the optional gain lifts the
detail's alpha, which exists for the one tab Leaf painted at half alpha (`system.png`, the source of
Aether's Themes tile) so its glyphs sit as solid as the other tabs' in the navigation row.

    python scripts/leaf_assets.py --analyze      # classification table over the source art
    python scripts/leaf_assets.py --convert      # write assets/aether/leaf/**
    python scripts/leaf_assets.py --sheet        # contact sheet, source next to result
    LEAF_SOURCE=<leaf asset dir>                  # override the source checkout

Thirty textures are converted; the files Aether does not use (login fields, the panorama, the social
buttons) are deliberately not. `SOURCE_NAMES` maps the outputs Leaf has no original for: Aether's
Themes tab (`button/themes.png`) is Leaf's `system.png` run through the same `detail` rule, so
`--convert` still rebuilds every shipped file from the Leaf checkout and nothing is hand-edited.
`assets/aether/leaf/NOTICE.txt` carries the attribution.

**An earlier version of the script gated its interior test on `alpha >= 200`, which is above every
plateau in this art, and therefore silently dropped *every* icon** — the generated
`button/setting.png` held 25 white pixels where the source has thousands. `--ascii` could not show
it (black glass and full transparency both print as blank), which is exactly why `--sheet` exists:
the result has to be looked at as an image, not as a text dump.

### Drawing it

- `Mc189Compat.drawTextureTinted(path, x, y, w, h, r, g, b, a)` — stretches one of Aether's own
  textures with a uniform tint. It builds the quad by hand because `Gui`'s helpers force the vertex
  colour back to opaque white, which would make every state change a no-op. `drawTexture` now
  delegates to it.
- `gui/leaf/LeafArt` — the asset names plus `draw` / `drawHovered`. A state is a **brightness**
  (enabled 1.00, idle 0.74-0.86, disabled 0.42), and hover is the same art drawn a second time at
  low alpha, so the *glass* brightens instead of the glyph being recoloured - Leaf could tint its
  opaque panels, translucent ones have to be redrawn.

## 2. Leaf geometry, as ported

| Screen | Leaf | Aether |
| --- | --- | --- |
| Nav tiles | `SystemButton(mod/cosmetic/location/setting, 430/650/1100/1320, 250, 170x106)` | Leaf's four keep its rectangles - modules 430, cosmetics 650, HUD (its `location`) 1100, settings 1320 - and Aether's Themes tile takes the free middle slot at 860; `button/<name>.png`, the wording is baked into the art |
| Themes picker | (Leaf has no theme screen) | Aether's own section in the Cosmetics shape: pills `300x90` at x=480 from y=400 pitch 100, 3 per page, scrollbar `(945, 400, 32, 400)`; the equipped palette is the bright pill |
| Module grid | `ModButton(mod, NAV_X[col], 400 + 220*row, 170x182)`, 8 per page | same, but its columns are Leaf's four (`430/650/1100/1320`) and not the nav row's x list - a column at the Themes tile's 860 would run under the scrollbar; `mod.png` + the name centered at `h/4`; top half toggles, bottom half opens settings |
| Card gear | `gear_small.png` at card-local (60, 110), 50x50, shifted 2 and grown 4 while hovered | identical, `ModuleCard` |
| Grid scrollbar | `ScrollBar(mods, 945, 400, 32, 400, 8)` | same; thumb = track / page count, whole-page wheel steps, wheel down = forward through the list |
| Backdrops | `ModSettings`/`CosmeticSettings` → `main.png`, `ModDetailSettings`/`ClientSettings` → `main_mod.png`, fullscreen stretched | same files |
| Detail screen | home tile `(640, 220, 80x80)`; toggles `100x60` at x=1120, selects `300x90` at x=920, sliders `255x90` at x=960, captions 410 / 210 / 250 to the left, rows from y=310 pitch 100 | same, generic over Aether's `Setting` rows |
| Cosmetics | entries `300x90` at x=480 from y=400 pitch 100, 3 per page; category pill at `(480, 700, 300x90)`; scrollbar `(945, 400, 32, 400)`; player model at `(1300, 800)`, 200 tall, mouse-clamped +-30 | same, plus the real `CosmeticLibrary` and Aether's `PlayerPreview` |
| Client settings | home tile; toggles `100x60` at x=1090 from y=310, captions 410 left; scrollbar `(1230, 310, 32, 460)` | the toggle column and the scrollbar are Leaf's; the field/profiles/actions are Aether's rows in the same idiom |

The panels were measured, not assumed: in the generated `main.png` the panel occupies design
x 348..1571, y 175..901, and in `main_mod.png` x 598..1321, y 178..901. Every ported control was
checked against those rectangles - that is why the module grid's category filter sits at y 806 (the
free strip under the cards) and why the settings rows' captions all start at x 710.

## 3. What is deliberately still Aether's

1. **Design space.** Leaf's `ScaleFixer` mixes `Toolkit` screen size with the window size and
   truncates to `int` per call. Aether keeps `GuiScale`: a 1080-unit-tall canvas whose width follows
   the window aspect, converted with `double` math in one place, mouse converted back with the same
   object. Leaf's fixed 1920x1080 composition is why the ported coordinates are the literal ones.
2. **The font.** Text is Aether's own `AetherFont` (TITLE/SECTION/BODY/SMALL/CAPTION) over the
   existing glyph renderer, not Leaf's `CustomFont`. The nav and tile wording lives in Leaf's art, so
   those are Leaf-sized; every caption Aether draws is Aether's type scale. (Leaf's `CustomFont` is
   set once to 50 design units, noticeably larger than Aether's rows.)
3. **Pages, not pixels.** The `PageBar` keeps Leaf's index paging *because it is Leaf's own screen
   behaviour*; `ModuleSearch` behind it is Aether's and stays pure and tested.
4. **Interaction hierarchy and GL discipline.** Every primitive goes through `Mc189Compat`, which
   restores colour/texture/blend; `PlayerPreview` wraps the entity render in push/pop matrix, its own
   scissor and the vanilla teardown sequence.
5. **Extra controls, in the same idiom.** The category filter on the modules screen, the profile
   manager on the settings screen and the module-name heading on the detail screen do not exist in
   Leaf. They are drawn from the same art and placed inside Leaf's panels (verified against the
   measured rectangles) rather than bolted onto the outside.
6. **The HUD editor.** Leaf's `ModPosSettings` is a background plus one toggle; Aether's editor
   keeps its drag/snap/scale/opacity machinery and its nav row, and uses the glass backdrop.
7. **Wheel direction, decided once.** `AetherGuiScreen` turns vanilla's wheel into one conventional
   delta (`scrollDelta(-wheel / 24)`: positive = forward/down, the sign flipped by the user's
   `Invert Scroll` preference) and the screens only ever read that sign. Leaf inverted per screen;
   copying that here, on top of the shell's own normalisation, made every paged list scroll
   backwards. The HUD editor takes the same preference for its scroll-to-scale/opacity.
8. **A fifth destination.** Leaf has four tabs, but Aether ships six palettes and a row of theme
   toggles buried in the modules grid is not discoverable, so Themes is a section of its own: the
   row keeps Leaf's four rectangles, the new tile takes the free slot between cosmetics and the HUD
   editor, and `GuiSection`'s declaration order is the tile order (the two lists move together).

## 4. Palette

`ThemePalettes.mono()` (surface `#06070A`, raised `#1A1B22`, accent **white** `#F2F2F5`) is the
default palette and also ships as the *Monochrome* theme module, so the default and the module cannot
drift apart. `AetherUi.applyTheme` derives every token from it, and `ACCENT_ON` now follows the accent
instead of a fixed green: on/off is carried by the art's own shape (a toggle's knob, a card's
brightness), so a second fixed hue would only fight the palette. The five coloured palettes remain
selectable.

## 5. Licensing

Leaf Client is GPLv3, the same licence as Aether. The components whose structure is adapted from
Leaf, and the derived art, carry attribution: source headers name `docs/GUI_REBUILD.md`, and
`assets/aether/leaf/NOTICE.txt` records the origin of every recoloured texture.
