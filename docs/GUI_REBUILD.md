# Aether In-Game GUI — the Leaf Client port

Status: implemented and building green. This document records how Aether's in-game screens came to
be a copy of Leaf Client 1.8.9's screens, what is deliberately still Aether's own, and how the copy
is verified. It is written as a decision record, not a plan.

Update 2026-10-02 (six-section rebuild + coordinate-space correction): the port below is where the
GUI *started*; today Modules is a `ModuleRow` list and Cosmetics is a `CosmeticCard` gallery -
Aether-native compositions that keep Leaf's art, backdrops and interaction split. The navigation is
six tiles with captions (Modules, Cosmetics, HUD Editor, Appearance, Profiles, Settings), and §3.8
records the coordinate-space rule the correction pass enforced. Everything in the art pipeline
section below still describes how the shipped textures are built.

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
| Nav tiles | `SystemButton(mod/cosmetic/location/setting, 430/650/1100/1320, 250, 170x106)` | six tiles, `170x106` at y **222**, x computed by `navX(index, count)` over Leaf's outer pair 430..1320: modules 430, cosmetics 608, HUD (Leaf's `location`) 786, appearance 964, profiles 1142, settings 1320; `navArt()` picks the artwork and every tile gets a caption at y 334 below it. The two Aether-added tiles are cut from `system.png` and stamped in `leaf_assets.py` (`_paint_appearance`'s contrast ring, `_paint_profiles`' stacked cards), so none can render blank |
| Appearance picker | (Leaf has no theme screen) | Aether's own: pills `300x90` at x=480 from y=400 pitch 100, 5 per page, `PageBar (945, 400, 32, 400)`; the equipped palette is the bright pill; the section caption sits at x=1000, clear of the pager |
| Modules | `ModButton(mod, NAV_X[col], 400 + 220*row, 170x182)`, 8 per page | replaced by a `ModuleRow` list (Glide-shaped, Aether's own): rows `1020x40` at x=430 from y=442, pitch 46, radius 8, 28-unit icon tile (r6), 34x18 switch, 24-unit gear, in a scissored `ScrollView`; `SearchBox (430, 386, 360)` and the `ChipBar` filter at y=388 sit on the strip above it |
| Grid scrollbar | `ScrollBar(mods, 945, 400, 32, 400, 8)` | Modules and Cosmetics now use `ScrollView` (continuous, wheel + drag, one normalised sign); the paged screens keep Leaf's `PageBar` |
| Backdrops | `ModSettings`/`CosmeticSettings` → `main.png`, `ModDetailSettings`/`ClientSettings` → `main_mod.png`, fullscreen stretched | same files |
| Detail screen | home tile `(640, 220, 80x80)`; toggles `100x60` at x=1120, selects `300x90` at x=920, sliders `255x90` at x=960, captions 410 / 210 / 250 to the left, rows from y=310 pitch 100 | same, generic over Aether's `Setting` rows; this screen has no nav row (`showsNav() == false`) |
| Cosmetics | entries `300x90` at x=480 from y=400 pitch 100, 3 per page; category pill at `(480, 700, 300x90)`; scrollbar `(945, 400, 32, 400)`; player model at `(1300, 800)`, 200 tall, mouse-clamped +-30 | replaced by a gallery: `CosmeticCard 220x128` (pitch 244x144) in 3 columns, list `(430, 494, 708x372)`; chip filters (All/Capes/Animated/Wings/Hats/Halos/Trails/Favorites/Custom) at y=440, `SearchBox (430, 386)` and the import button + popover beside it, status line at y=876, and `PlayerPreview` still shows what is worn |
| Profiles | (Leaf has no profiles) | Aether's own: name field `400x67` at x=920 from y=410 pitch 100, Save/Apply/Delete in Leaf's tile idiom, `PageBar (1230, 410, 32, 400)` past five rows |
| Client settings | home tile; toggles `100x60` at x=1090 from y=310, captions 410 left; scrollbar `(1230, 310, 32, 460)` | the toggle column stays Leaf's but starts at y **410** (clear of the nav row) and pages through `PageBar (1230, 410, 32, 400)`; the rows are Aether's real preferences, and the status flash draws at y 366 - the strip under the captions, not inside the nav band |

The panels were measured, not assumed: in the generated `main.png` the panel occupies design
x 348..1571, y 175..901, and in `main_mod.png` x 598..1321, y 178..901. Every ported control was
checked against those rectangles - that is why the search/filter strip sits at y 386/388 above the
module list (442..866), why the settings rows' captions all start at x 710, and why no screen
draws into the navigation band: the tiles occupy y 222..328, their captions 334..346, and content
starts at y 410 (paged screens) or 442 (module rows).

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
5. **Extra controls, in the same idiom.** The chip filter and search on the modules screen, the
   import popover on Cosmetics and the module-name heading on the detail screen do not exist in
   Leaf. They are drawn from the same art and placed inside Leaf's panels (verified against the
   measured rectangles) rather than bolted onto the outside.
6. **The HUD editor.** Leaf's `ModPosSettings` is a background plus one toggle; Aether's editor
   keeps its drag/snap/scale/opacity machinery and its nav row, and uses the glass backdrop.
7. **Wheel direction, decided once.** `AetherGuiScreen` turns vanilla's wheel into one conventional
   delta (`scrollDelta(-wheel / 24)`: positive = forward/down, the sign flipped by the user's
   `Invert Scroll` preference) and the screens only ever read that sign. Leaf inverted per screen;
   copying that here, on top of the shell's own normalisation, made every paged list scroll
   backwards. The HUD editor takes the same preference for its scroll-to-scale/opacity.
8. **Six destinations, computed not hardcoded.** Leaf has four tabs; Aether ships six sections
   (Modules, Cosmetics, HUD Editor, Appearance, Profiles, Settings) and `GuiSection`'s declaration
   order is the tile order. The row's x positions come from `navX(index, count)`: Leaf's outer pair
   (430 / 1320) anchors it and the tiles are pitched evenly between (178 units at six), so adding
   or reordering a section needs no new constant. Themes are not a destination at all any more - a
   theme is configuration worn on Appearance and owned by `ThemeManager`, which is why the old
   five-tile Themes layout and its tile in the modules grid are gone.
9. **One coordinate space, end to end.** Everything is laid out in design units and converted at
   the draw call through `GuiScale` (`x/y/w/h`); the mouse comes back through `GuiScale.mouseX/Y` -
   the same object, the same factor, so a hit test and the drawing it belongs to can never
   disagree. The correction pass's `ModuleRow` bug was exactly the violation: design constants
   added to a `gx()/gy()` result without `GuiScale.w()/h()`, which produced 84-unit icon tiles and
   a 126-unit name offset, and `AetherUi`'s glyph helpers ignored their `size` argument entirely.
   Both are fixed and the result measured from screenshots at 1280x720, 1920x1080 and 2560x1440:
   the same design lands at the same design coordinates at every 16:9 resolution.

## 4. Palette

`ThemePalettes.mono()` (surface `#06070A`, raised `#1A1B22`, accent **white** `#F2F2F5`) is the
default palette and also the default *theme*: `ThemeManager.DEFAULT_THEME_ID` (`theme.monochrome`)
is built from that same definition, so the default and the theme cannot drift apart - themes are
configuration now, not modules. `AetherUi.applyTheme` derives every token from it, and `ACCENT_ON`
now follows the accent instead of a fixed green: on/off is carried by the art's own shape (a
toggle's knob, a card's brightness), so a second fixed hue would only fight the palette. The five
coloured palettes remain selectable on Appearance.

## 5. Licensing

Leaf Client is GPLv3, the same licence as Aether. The components whose structure is adapted from
Leaf, and the derived art, carry attribution: source headers name `docs/GUI_REBUILD.md`, and
`assets/aether/leaf/NOTICE.txt` records the origin of every recoloured texture.
