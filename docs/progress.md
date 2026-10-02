# Aether — Soar v4 Port + Leaf Visual Language — Implementation Tracker

This is the live tracker. It is updated after every phase. Nothing in here is aspirational
without a checkbox: if it is `[x]`, the code exists and the build was green when it was checked.

Rules this tracker enforces (from the task brief):

- Aether is the base. Leaf is a *visual* reference only. Soar v4 is a *behaviour* reference only.
- No parallel UI / module / HUD / setting systems. Extend what exists.
- No dead settings. No placeholder runtime UI. No permanent debug visuals.
- Every animation is frame-rate independent.
- `docs/progress.md`, `docs/architecture.md`, `docs/stop.md` are updated after every phase.

---

## CURRENT PHASE

**GUI/cosmetics overhaul: themes-as-config refactor + new screens + the screenshot-driven
correction pass** (the Leaf UI copy, Phases 1-6, and the Themes/scroll/wordmark follow-up are
complete; see below). The refactor is committed as `1fcb5b1`; the correction pass over the defects
that verification exposed (`ModuleRow`/`ChipBar`/`SearchBox` mixed units, the glyph helpers
ignoring their size, blank nav art, the nav caption/content collision, the Appearance
caption/`PageBar` overlap, the walker's mid-walk state reset) is applied, green, and measured away
in fresh 1920x1080 and 1280x720 walks. It is **not committed** yet. Next exact tasks are recorded
in `docs/stop.md` (CURRENT PROBLEM → NEXT EXACT STEPS); Phase 7 — Block Info comes after.

---

## Reference repositories

| Repo | Use | State |
| --- | --- | --- |
| `Alex07lol/aether` | base codebase | local checkout, branch `master` |
| `kazubon12/Soar-Client-v4.0` | module behaviour / settings / animation reference | read-only reference |
| `Lefiy/Leaf-Client` | visual + interaction reference | cloned at `/tmp/leaf-client-ref` (session-local) |

The Aether GUI has **already** been rebuilt on the Leaf 1.8.9 screen composition
(commits `2a1766e`, `5ebf6d2`; see `docs/GUI_REBUILD.md`). Phase 18 of this plan is therefore a
*polish/consistency* pass over that work, not a rebuild.

---

## Phase 1 — Audit

- [x] Audit Aether module registry / settings / HUD / mixins / Forge bridge
      → findings in `docs/architecture.md`
- [x] Audit Aether GUI (Leaf rebuild already landed: `gui/AetherGui`, `AetherModScreen`,
      `AetherCosmeticScreen`, `AetherHudEditorScreen`, `AetherClientSettingsScreen`,
      `gui/leaf/*` components)
- [x] Audit Soar v4 module list vs Aether module list → **`docs/architecture.md` §15**: every Soar
      module is marked HAVE / MISSING+phase / SKIP+reason against Aether's 59 registered ids
- [x] Found: `graphics.item_physics` and `graphics.motion_blur` are unregistered module classes
      (dead code — Phase 14 registers them with real behaviour or removes them)
- [x] Audit Soar v4 animation usage (`SimpleAnimation`, `EaseBackIn`, per-module anim state)
      → the recurring shape is *persistent scalar + target + time-based step*, which is what
      Phase 2 implements generically
- [x] Audit Leaf 1.8.9 GUI (`screen/ui/*`, `FontUtils`) — already consumed by the Leaf rebuild
- [x] Create `docs/progress.md`, `docs/architecture.md`, `docs/stop.md`

### Bugs found during the audit

- [x] **Freelook jitter — root cause found and proven from the decompiled Forge source.**
      Aether consumed mouse deltas from Forge's `MouseEvent`. Forge posts `MouseEvent` from
      `Minecraft.runTick()` (`ForgeHooksClient.postMouseEvent()` inside the `while (Mouse.next())`
      loop), and `Minecraft.runGameLoop()` calls `runTick()` **once per elapsed game tick**
      (`for (int j = 0; j < this.timer.elapsedTicks; ++j) this.runTick();`) — i.e. ~20 Hz.
      The camera therefore advanced in 20 Hz steps against a frame-rate render. Worse at high FPS.
      Vanilla's own look path is per frame: `EntityRenderer.updateCameraAndRender` →
      `MouseHelper.mouseXYChange()` → `setAngles`, and `updateCameraAndRender` is called once per
      frame from `runGameLoop`. Fixed in Phase 3.
- [x] **Cancelling `MouseEvent` broke vanilla input.** `Minecraft.runTick()` does
      `if (ForgeHooksClient.postMouseEvent()) continue;` — a cancelled `MouseEvent` skips
      `KeyBinding.setKeyBindState`/`onTick` and the wheel handling for that event. While freelook
      was held, left/right click and hotbar scroll were being swallowed. Fixed in Phase 3
      (the event is no longer cancelled; the rotation freeze mixin is the guard).
- [ ] **Freelook camera collision uses the wrong direction (known, not yet fixed).**
      `EntityRenderer.orientCamera` raytraces the third-person pull-back along
      `entity.rotationYaw/rotationPitch` (`f1`/`f2`), while the camera *orientation* comes from the
      `CameraSetup` event. Under freelook the two disagree, so the camera can clip into geometry
      and the pull-back distance (`d3`) can pop when the player is near walls. Needs a
      `@Redirect` on the `rayTraceBlocks` call in `orientCamera` (see `docs/stop.md`).
- [ ] No shared animation system existed (`graphics/*` only holds pure maths for specific
      features; `cosmetic/CosmeticAnimation` is a sprite-sheet frame stepper). Fixed in Phase 2.

---

## Phase 2 — Reusable animation infrastructure

- [x] `dev.aether.animation.Easing` — easing curves (`LINEAR`, `EASE_IN_QUAD`, `EASE_OUT_QUAD`,
      `EASE_OUT_CUBIC`, `EASE_OUT_QUART`, `EASE_IN_OUT_CUBIC`, `EASE_OUT_EXPO`, `EASE_OUT_BACK`,
      `EASE_IN_BACK`), each a pure function of `t` in `[0,1]`
- [x] `dev.aether.animation.FrameClock` — one frame delta + frame index per rendered frame
      (published from `TickEvent.RenderTickEvent` START), so no animation needs its own clock
- [x] `dev.aether.animation.Anim` — persistent animated scalar: target, duration, easing,
      `update()` (guarded against double-advance in the same frame), `set`, `jumpTo`, `settled`,
      `reverse`, `progress`
- [x] `dev.aether.animation.AnimationMath` — pure helpers (`clamp01`, `lerp`, `lerpColor`,
      `approach`, `frameStep`) used by HUD/GUI and unit tested without Minecraft
- [x] `AnimationTest` self test (main-style, picked up by `coreSelfTests`)

## Phase 3 — Freelook jitter

- [x] `MixinFeatures.Mouse` — frame delta published by the new mixin
- [x] `MouseHelperMixin` — `@Inject(at = RETURN)` on `MouseHelper.mouseXYChange()`, the one
      per-frame mouse intake in vanilla; reads the public `deltaX`/`deltaY` fields
- [x] `mixins.aether.json` — `MouseHelperMixin` added to the client list
- [x] `ForgeClientEventBridge` — freelook consumes the frame delta at the camera hook instead of
      the tick-rate `MouseEvent`; `MouseEvent` is no longer cancelled; pending deltas are dropped
      on activation/release so no stale movement is applied
- [x] Preserved settings: `keybind`, `activation` (Hold/Toggle), `sensitivity`, `invert_x`,
      `invert_y` — untouched

## Phase 4 — Keystrokes animation

- [x] Per-key animation state (`press` progress + `held` flag) in the HUD renderer, array-indexed
      (no map lookups, no per-frame allocation, no shared animation value)
- [x] Press: fast animated transition (90 ms, ease-out-cubic) to the pressed colour + a press
      shrink, so the press is visible as movement and not only as a colour swap
- [x] Release: animated return over the module's own `fade_time` (`0` now means "snap", which is
      what the setting's own floor says; the old hard-coded 120 ms fallback is gone)
- [x] Existing settings all preserved and all still authoritative
- [x] The HUD editor now draws the **real** keystrokes component (W and LMB shown held) instead of
      nothing at all — the element had a selection box but no preview
- [x] Dead state removed: the two `HashMap<String,…>` key maps and the fallback constant are gone
- [ ] Visual confirmation at 60/120/240 FPS (needs a live `runClient` session)

---

## Phase 5 — Target Info

- [x] `dev.aether.module.impl.hud.TargetInfoModule` — id `hud.target_info`, category HUD,
      registered in `HudModules`; a `HudElement` was added in `BuiltInModules` (260, 164)
- [x] Settings (all fourteen read by the renderer / layout): `show_skin`, `show_name`,
      `show_health_bar`, `show_health_text`, `health_mode` (Value / Percent / Both),
      `show_background`, `bar_width`, `animation_time`, `damage_flash_time`, `background_color`,
      `text_color`, `bar_color`, `bar_background_color`, `damage_color`
- [x] Real skin: the player's own skin texture drawn as the head face + hat overlay
      (`Mc189Compat.skinLocation` + the new `Mc189Compat.drawTextureRegion`), not an empty box
- [x] Enter/exit animation: `Anim` on open/close (`animation_time`, ease-out-cubic), fading **and**
      growing into place; the card keeps drawing the last target while it closes
- [x] Damage animation: edge-triggered (the hurt timer's first frame, or a real health drop), one
      flash per hit for exactly `damage_flash_time`, tinting the card towards `damage_color`
- [x] Health animation: the bar and the number both follow a smoothed health value
      (`AnimationMath.approach`, rate derived from `damage_flash_time`) instead of snapping
- [x] Target source: `Minecraft.objectMouseOver.entityHit` (the same ray trace the block outline
      uses), falling back to the last entity hit within 3 s; dropped on a world change or a death
- [x] HUD editor: real component preview (the local player's own skin and name) and a measuring box
      computed by the *same* layout method the card uses
- [x] The pure rules (health line per `health_mode`, bar fill fraction) live in
      `dev.aether.hud.TargetHealthText` and are unit tested by `TargetHealthTextTest`, so the three
      modes cannot rot into dead settings
- [ ] Live confirmation (needs `runClient`: aim at a mob, hit it, lose it)

## Phase 18 (pulled forward) — Leaf UI copied exactly, in transparent black and white

The brief changed to "also copy the leaf client UI exactly with colour changes of transparent black
and white", so Phase 18's polish pass became the whole job: Leaf's own art, at Leaf's own
coordinates, in a two-colour palette.

- [x] `scripts/leaf_assets.py` rebuilt as a plateau-driven pipeline (`--analyze`, `--ascii`,
      `--convert`, `--sheet`) after measuring the source art: flat alpha plateaus 128 (panels), 255
      (glyphs), 192 (toggle knobs), 38/142 (backdrops); the baked shadow shares the icons' dark tone,
      so the alpha plateau - not luminance - is what separates a glyph from its shadow
- [x] **Bug found and fixed:** the first script gated its interior test on `alpha >= 200`, above every
      plateau, so it silently dropped every icon (generated `button/setting.png` had 25 white pixels
      where the source has thousands). `--sheet` was added because `--ascii` cannot show it
- [x] 29 textures converted to `assets/aether/leaf/**`, verified numerically (white/opaque counts per
      file) and packaged in the jar (33 entries incl. the notice)
- [x] `assets/aether/leaf/NOTICE.txt` - GPLv3 attribution for the derived art
- [x] `Mc189Compat.drawTextureTinted` (tint reaches the vertex colour; `Gui`'s helpers would reset it)
      and `gui/leaf/LeafArt` (asset names, `draw`, `drawHovered`)
- [x] Components rebuilt on the art: `NavButton` (Leaf's `SystemButton`), `ModuleCard` (`mod.png` +
      `gear_small.png` at Leaf's (60, 110) 50x50, hover shift 2 / grow 4, name at `h/4`, top half
      toggles, bottom half opens settings), `PageBar`, `SelectButton`, `CosmeticEntry`, `LeafToggle`
      (`true.png`/`false.png`), `LeafTextBox` (`field/search.png`), `LeafBar` (`bar_main.png` +
      `bar_point.png`), keybind pills
- [x] `AetherGuiScreen` draws Leaf's fullscreen backdrop art per screen (main / main_mod, exactly as
      Leaf assigns them) and Leaf's nav tiles from `button/<name>.png`; the gradient backdrop and the
      drawn Aether logo were removed, Leaf-exact
- [x] Every ported control checked against the **measured** panel rectangles (`main.png` panel =
      design x 348..1571, y 175..901; `main_mod.png` = x 598..1321, y 178..901): the modules screen's
      category filter moved to the free strip under the cards (y 806), the text-field rows moved into
      the choice column so all captions line up at x 710, and the profile rows (field + Save, Apply +
      Delete) were re-placed inside the narrow settings panel
- [x] Palette: `ThemePalettes.mono()` is the default **and** a Monochrome theme module, so the
      default and the module share one palette; `ACCENT_ON` follows the accent instead of a fixed
      green; the HUD editor's red/green bar states became the accent and the secondary text tone
- [x] Dead code: unused `Mc189Compat.hasResource` removed, and `ThemePalettes.names()` removed here
      too - it came back briefly for the Themes picker's category filter and went again when that
      filter was cut for showing nothing (see the follow-up section below); nothing reads it now
- [x] `compileJava`, `coreSelfTests` (29/29; registry now 61 modules) and `scripts/verify.sh`
      (build + jar proof) green
- [ ] Live confirmation in `runClient` (the screens have not been looked at on a running client)

## Phase 6 — Mouse Display (`hud.mouse_display`)

- [x] `MouseDisplayModule` (id `hud.mouse_display`, category HUD, registered in `HudModules`);
      `HudElement` added in `BuiltInModules` at (260, 128)
- [x] Settings: `show_background`, `show_direction`, `size` (14-48, step 2), `movement_speed`
      (40-1000, step 20), `background_color`, `indicator_color`
- [x] **Delta without stealing:** `ForgeClientEventBridge.onCameraSetup` drains
      `MixinFeatures.Mouse` once per frame into cached `frameDeltaX/Y` fields (where it always
      drained); `frameMouseDelta()` now exposes that drained copy. The HUD reads the cache, so the
      freelook camera and the widget both see every flick and neither can consume the other's
      movement — the one-drain-many-readers shape the stop notes called for
- [x] Movement is the pure `dev.aether.hud.MouseIndicator` maths: the raw delta sets a direction-
      preserving target offset clamped to the pad's radius, and `AnimationMath.approach` (rate from
      `movement_speed`) travels towards it — so stillness is the same code springing back to centre.
      No teleporting, no per-frame allocation beyond the tiny result arrays
- [x] Renderer in `ForgeHudRenderer`: pad + travel ring + disc dot + optional direction word,
      driven by `FrameClock`, scaled/faded by the element's own `scale()`/`opacity()`; real editor
      preview with a fixed representative flick; `getDimensions` matches the drawn size (incl. the
      direction line)
- [x] `MouseIndicatorTest` under `coreSelfTests` (convergence on the rim, diagonal vector clamp,
      spring-back, 100 ms ≈ 20×5 ms, no NaN on zero delta, eight-way direction words)
- [ ] Live confirmation (needs `runClient`: wiggle the mouse, watch the dot lean and recentre)

## Leaf UI follow-up — Themes section, wheel direction, entry-screen wordmark

Asked for directly (not one of the numbered phases). Every item below was checked on a live client
with the screenshot walker, at 1920x1080, and the numbers are in `docs/stop.md` TEST STATUS.

- [x] **Themes is a section of its own** (`GuiSection.THEMES`; five sections). `AetherThemesScreen` is
      the Cosmetics list without the player preview: 300x90 pills at x=480 from y=400, pitch 100,
      three per page, scrollbar (945, 400, 32, 400), the whole screen paging on the wheel. The list is
      built by scanning the registry for `ThemeModule`s, so a seventh palette pages rather than
      overflows and the screen needs no change; the equipped theme is the bright pill, and clicking
      one enables that module (the registry's group rule makes the palettes exclusive) and saves
      through the client's own path
- [x] No category/family filter on that screen: with one palette per family it could not change what
      the list shows, and an invented grouping over six pills is a control that does nothing. The
      filter and its only data (`ThemePalettes.names()`) were cut rather than shipped
- [x] **Navigation order is the tile order.** `GuiSection`'s declaration order pairs with
      `NAV_X = {430, 650, 860, 1100, 1320}`, so Leaf's own tabs keep Leaf's rectangles (modules 430,
      cosmetics 650, HUD/`location` 1100, settings 1320) and Aether's Themes tile takes the free slot
      at 860. The first cut declared THEMES ahead of HUD, which silently moved Leaf's HUD tab to 860
      and swapped the two tabs' art - caught by measuring the live screenshots (exactly one bright
      tile per screen, at 430/650/860/1100/1320 for modules/cosmetics/themes/HUD/settings)
- [x] **The module grid keeps Leaf's columns** (`GRID_X = {430, 650, 1100, 1320}`, deliberately not
      `NAV_X`): reusing the nav row's x list put a card column under the page scrollbar at x=945
- [x] **Aether's Themes tile art** is Leaf's `system.png` through the same rule under our own name
      (`SOURCE_NAMES` in `scripts/leaf_assets.py`, so `--convert` rebuilds all 30 textures); it is the
      one tab Leaf painted at half alpha, so the `detail` rule gained an optional alpha gain and its
      white glyphs now sit as solid as its neighbours' - before that the current screen was the
      *dimmest* tile in the row
- [x] **Wheel direction fixed once, in the shell.** `AetherGuiScreen.handleMouseInput` normalises the
      sign (`scrollDelta(-wheel / 24)`, positive = forward/down) and screens read `delta > 0 ->
      onScroll`. Before, the shell negated vanilla's wheel and every screen negated it again, so all
      paged screens ran backwards
- [x] **`Invert Scroll` preference** (`preference.invert_scroll` + `ClientPreferences.invertScroll()`)
      with a Leaf toggle on the client settings screen; `scrollDelta` is the only place the choice is
      applied, so the module grid, cosmetics, themes, module settings, client settings and the HUD
      editor all follow it. (Round-tripped in `run/config/aether/client.json` on the dev run)
- [x] **The Aether wordmark is back on the entry screen**: `AetherModScreen.drawBranding()` draws
      `aetherlogo.png` at design (430, 292) with the `AETHER` title in the accent beside it, in the
      panel corner Leaf leaves empty above the card grid
- [x] `ClientPreferencesTest` extended (defaults, round trip, `SECTIONS[2] == "Themes"`,
      `SECTIONS[3] == "HUD Editor"` - the tile order is pinned in a test); `coreSelfTests` **30/30**,
      **62 modules / 214 settings**, `scripts/verify.sh` green end to end (jar carries all 30 leaf
      textures incl. `button/themes.png`)
- [x] Screenshot-verified live: 11 shots including `debug-aether-themes`, `-themes-equipped`,
      `-themes-scrolled`; the picker was reset to the default palette first, a real click on the first
      pill lit it, and the choice landed in the config - so selection works end to end, not just on
      screen

## GUI/cosmetics overhaul — themes-as-config refactor + new screens (in progress, uncommitted)

Spec §3-§7: themes are configuration, not modules — fix the registration metadata instead of
filtering in screens. `GuiSection` is now six sections (Modules/Cosmetics/HUD Editor/Appearance/
Profiles/Settings); nav layout is the responsive `navX(index,count)` + `navArt()`.

- [x] Full theme refactor: `ThemeManager`, `ThemeDefinition`, `AetherTheme` rewrite, `AetherClient`
      wiring (`themes()`, config load/save); deleted theme modules / `ThemeModule` /
      `ModuleKind.THEME`, registry guard removal; `defaultTheme = AetherTheme.of(themes.active())`
- [x] New screens: `AetherAppearanceScreen` (theme pills via `ThemePill`, `LIST_X` 480 / `LIST_TOP`
      400 / pitch 100 / 5 per page / `PageBar` 945/400/32x400) and `AetherProfilesScreen` (field +
      Save, Apply + Delete rows; profiles stripped from `AetherClientSettingsScreen`)
- [x] `AetherModScreen` is a `ModuleRow` list (search `SEARCH_Y` 386, `ChipBar` filter `FILTER_Y`
      388, list 430/442/1020x424, pitch 46) with debug hooks (`debugSearch`, `debugSelectCategory`,
      `debugToggleFirst`); `SearchBox.setText`; walker `Step` enum, `resetThemes()`, resize,
      heartbeat, 3600-tick safety
- [x] Art pipeline: `button/themes.png` → `appearance.png`, `profiles.png` added
      (`SOURCE_NAMES`/`ASSETS` updated, `--convert` run, 31 assets)
- [x] Tests green: `ThemeManagerTest` new (manager basics, incl. `resetToDefault()` first in
      `clientResolution`, config round trip, module-era migration); `ThemeModuleTest.java` deleted
      (+ stale `.class` files removed from build dirs); `ModuleVisibilityTest` rewritten (no
      `theme.*` entries, no `THEMES` category); `ClientPreferencesTest` rewritten (six sections,
      `sectionIndex("Settings")==5`, `normalizeSection("themes")=="Appearance"` folding the old
      `migrateSection`); `coreSelfTests` **35/35**, `scripts/verify.sh` green, jar proven clean
      (no stale theme classes, `appearance.png`+`profiles.png` present, `themes.png` absent)
- [x] `runClient -PaetherDebugShots=1920x1080` (JDK 8 `.gradle-dist/jdk8u504-b01` — JDK 25 fails
      `:makeStart` on source level 6) completed; all 19 walker screenshots written
      (`appearance-equipped` proves end-to-end selection). Old pre-refactor shots kept at
      `$env:TEMP\aether-old-shots` for comparison
- [x] **Applied — the coordinate-space correction pass.** The root defect was that drawing
      primitives take GUI pixels while components stored design units; every design constant that
      reaches a primitive now passes through `GuiScale.w()/h()`, and hit testing stays in design
      units. `ModuleRow` (radius, slide, 28-unit icon tile, text offsets, trim widths, 34x18 switch
      with a r=7 knob, 24-unit gear), `ChipBar` (render height/radius/padding, `indexAt`) and
      `SearchBox` (radius, padding, 11-unit magnifier) are converted.
- [x] **The deeper bug behind it:** `AetherUi`'s glyph helpers (`drawSearchGlyph`,
      `drawModuleGlyph`, `drawGearGlyph`) ignored their `size` parameter and always drew fixed
      pixels, so `ModuleRow`'s tile could not be fixed at the call site. `drawSearchGlyph` gained a
      scaled `size`; the other two now scale through a private `Glyph` helper. `AetherUi.drawBadge`
      used `stringWidth(null,...)+10`-tall fixed pixels (the oversized EQUIPPED pill); it now
      measures through `AetherFont`/`GuiScale`, and `CosmeticCard` computes the matching size.
- [x] Nav geometry: `NAV_Y` 250 -> **222** (captions end by y 351, content starts at 386, no
      collision); `AetherAppearanceScreen`'s caption moved to `LIST_X+520` so it clears the PageBar
      at design x 945..977.
- [x] Nav art: `scripts/leaf_assets.py` gained `_paint_appearance` (the half-filled contrast ring),
      `_paint_profiles` (two stacked cards, back card knocked out) and `stamp_tab_glyph`
      (4x supersampled, 60% of the tile height, LANCZOS, pasted through its alpha mask) applied to
      `button/appearance.png` + `button/profiles.png`; `--convert` rewrote the 31 assets and both
      tiles now carry a white glyph (verified by per-tile pixel counts).
- [x] Walker: `resetThemes()` + new `resetCosmetics()` (clears every `CosmeticType` slot) moved into
      the `RESIZE` step, i.e. the start of the walk, instead of mid-walk; new
      `COSMETICS_SEARCH`/`SHOOT_COSMETICS_SEARCH` steps and a `debug-aether-cosmetics-search` shot;
      `TOGGLE_MODULE` now calls `AetherModScreen.debugToggleVisible()` (toggling the first row inside
      the current viewport - `debugToggleFirst` toggled an off-screen row after scrolling, so the
      shot was byte-identical to the scrolled one); `AetherCosmeticScreen.debugSearch(String)` wired
      to `SearchBox.setText`.
- [x] Re-verified: `compileJava` + `coreSelfTests` **35/35** + `scripts/verify.sh` green with the jar
      proof; `runClient -PaetherDebugShots=1920x1080` wrote 20 shots (the new cosmetics-search state
      included) and every documented defect was measured away in the PNGs - e.g. the 28-unit icon
      tile is 28 px with a 16 px glyph at 1080p, the EQUIPPED badge is 16 design units tall, and
      `debug-aether-modules-scrolled` vs `-toggled` now differ in the row box (436,444)-(1404,468)
      that previously matched byte for byte. A `1280x720` walk then re-measured the same layout at
      the 2/3 framebuffer scale (nav tile 0 at px 287, icon glyph at px 294..305, captions clear of
      content). Shots kept in `run/screenshots/1080p/` and `run/screenshots/720p/`.
- [x] `graphics.item_physics` / `graphics.motion_blur` (brief §56): **already deleted, not
      registered** - `docs/MODULE_AUDIT.md` records the six modules whose switches could not be
      backed by a hook (`item_physics`, `motion_blur`, `crosshair_editor`, `gui_tweaks`,
      `screenshot_manager`, `scroll_tooltips`); `graphics.custom_crosshair` covers the crosshair case.
      No such class remains in `src/`, and `ARCHITECTURE.md`'s comparison table no longer claims the
      classes exist.
- [x] **2560x1440 responsive check (brief §38):** the third walk measured the same design layout at
      the 4/3 framebuffer scale - nav tile 0 frame at design x 432 y 222, row text at 450..480,
      captions/content clear, gallery card columns at their design positions. All three requested
      resolutions (720p/1080p/1440p) now have a walked, measured screenshot set.
- [x] **Walker module-state determinism:** the walker restored themes/cosmetics but not module
      switches, so one run's toggle leaked into the next run's images (and the user's config). It
      now snapshots every switch at launch and restores at walk start *and* walk end - proven by
      two back-to-back 1080p walks (10/20 shots byte-identical, switch state pixel-identical in all
      four module shots).
- [x] **Status-flash collision:** the Settings and Profiles "saved" flashes drew at design y 250,
      inside the nav tile band; moved to y 366 (free strip under the captions, above the rows).
- [x] **Dead code:** `ModuleCard` and `CosmeticEntry` (the components the row list and the gallery
      replaced) deleted; `ARCHITECTURE.md` and `GUI_REBUILD.md` re-synced to the six-section layout,
      the coordinate-space rule, `ThemeManager` ownership and the deleted module list.

## Phases 7-19 (planned)

- [ ] Phase 7 — Block Info enter/exit transition around real block data
- [ ] Phase 8 — Damage Tint animated intensity (threshold preserved)
- [ ] Phase 9 — Smooth zoom (already partially done via `ZoomMath.smooth` + `EntityRendererMixin`;
      remaining: per-frame clock instead of per-call `nanoTime`, and scroll target smoothing)
- [ ] Phase 10 — Animated hotbar selection indicator (GUI render hook)
- [ ] Phase 11 — Animated inventory open/close + `Click out of Container` + `Transparent
      Background` + `Prevent Potion Shift`
- [ ] Phase 12 — Chunk Animator (RenderChunk timestamps in the world render path, not the HUD)
- [ ] Phase 13 — Missing HUD modules (Health, Horse stats, Saturation, Session, Stopwatch,
      Time/Weather display, Potion counter, Compass upgrade, Player/Name display, Music info,
      Rearview, Image display)
- [ ] Phase 14 — Missing render modules (Clear glass/water, Glint colour, Hitbox, Minimal
      bobbing/damage shake, TNT timer, UHC overlay, Overlay/Tab editor, Chunk borders, Blood
      particles, Damage particles, Small held items, Item physics review)
- [ ] Phase 15 — Missing PvP/utility modules (Hit delay fix, Raw input, Slow swing, Taplook,
      Bow zoom, Sound modifier)
- [ ] Phase 16 — Performance expansion (batch rendering, chunk delay, entity filters, particle
      limit, font shadow, block effects) — each one a real optimisation or not shipped
- [ ] Phase 17 — HUD module polish (armour icons, potion icons, real previews in the editor)
- [ ] Phase 18 — Leaf visual pass over the existing Aether screens (black transparent glass,
      spacing, states) — polish only, no rebuild
- [ ] Phase 19 — Full regression test + docs

---

## Build / test log

| Date | Phase | Result |
| --- | --- | --- |
| 2026-09-30 | 2/3/4 | `compileJava` green; `coreSelfTests` **28/28** (27 pre-existing + `AnimationTest`); `scripts/verify.sh` green end to end (jar proof: refmap searge+notch, every compiled mixin present in the config, manifest bootstrap, reobf) |
| 2026-09-30 | 5 | `scripts/verify.sh` green again: **60 modules / 208 settings / 54 ranges / 26 choice lists**, `coreSelfTests` **29/29** (`TargetHealthTextTest` added), jar proof still OK |
| 2026-09-30 | 18 (pull-forward) | Leaf UI copy + art pass: `leaf_assets.py --convert` wrote 29 textures (verified numerically), `compileJava` green, `coreSelfTests` **29/29** (**61 modules** - the Monochrome theme), `scripts/verify.sh` green end to end, jar contains `assets/aether/leaf/**` |
| 2026-09-30 | GUI verified live | `runClient -PaetherDebugShots=1920x1080` walked every screen; panel fit, white glyphs, state brightness and the enabled/disabled card split all confirmed from the screenshots (see `docs/stop.md` TEST STATUS 0) |
| 2026-09-30 | 6 | Mouse Display: `compileJava` green, `coreSelfTests` **30/30** (`MouseIndicatorTest` added), **62 modules / 214 settings**, `scripts/verify.sh` green end to end |
| 2026-10-01 | Leaf UI follow-up | Themes section, wheel direction + `invert_scroll`, wordmark: `coreSelfTests` **30/30**, **62 modules / 214 settings / 56 ranges / 26 choice lists**, `scripts/verify.sh` green end to end, jar carries all 30 `assets/aether/leaf/**` textures |
| 2026-10-01 | Leaf UI follow-up (live) | `runClient -PaetherDebugShots=1920x1080` walked 11 screens (three new themes shots); the nav row's order/brightness, the pill selection, paging and the scrollbar thumb were measured out of the PNGs. The walker now forces the framebuffer to the requested size, so shots are 1:1 design units (see `docs/stop.md` TEST STATUS 0) |
| 2026-10-02 | Themes-as-config refactor | `compileJava` green, `coreSelfTests` **35/35** (`ThemeManagerTest` added), `scripts/verify.sh` green end to end, jar proven clean; `runClient -PaetherDebugShots=1920x1080` (JDK 8) wrote 19 screenshots — verification exposed the `ModuleRow` mixed-unit bug + nav/caption defects (see `docs/stop.md` CURRENT PROBLEM), fixes identified but not yet applied |
| 2026-10-02 | Correction pass (uncommitted) | `compileJava` green, `coreSelfTests` **35/35**, `scripts/verify.sh` green end to end incl. the jar proof; `runClient -PaetherDebugShots=1920x1080` wrote 20 shots (new cosmetics-search state) and a `runClient -PaetherDebugShots=1280x720` walk re-measured the same design layout at the 2/3 framebuffer scale. Every defect the previous row lists was measured away in the PNGs: 28 px icon tile with a 16 px glyph, the switch/gear at their design positions, the `EQUIPPED` badge 16 units tall, nav captions clear of content, the Appearance caption right of the PageBar, and `modules-scrolled` vs `modules-toggled` now differing in the toggled row |

Proof that the new mixin binds in production as well as dev: `build/tmp/compileJava/compileJava-refmap.json`
contains `dev/aether/forge189/mixin/MouseHelperMixin → mouseXYChange -> Lnet/minecraft/util/MouseHelper;func_74374_c()V`.

### Note on the documentation file names

This checkout is case-insensitive (Windows/OneDrive), so `docs/architecture.md` and the
pre-existing `docs/ARCHITECTURE.md` are **the same file**; git therefore reports the architecture
doc as a modification of `ARCHITECTURE.md`. That is expected, not a missing file. The previous
40-line aspirational version (which described `forge-1.7.10`, `launcher` and `cloud` layers that do
not exist in this repository) was replaced by the real architecture; its still-true design rules
are preserved in §13b.

## Remaining work (this pass)

- Commit the correction-pass changeset (13 files: `ModuleRow`, `ChipBar`, `SearchBox`, `CosmeticCard`,
  `AetherUi`, `AetherGuiScreen`, `AetherModScreen`, `AetherAppearanceScreen`, `AetherCosmeticScreen`,
  `AetherVisualDebugHook`, `scripts/leaf_assets.py`, the two regenerated nav tiles) once the user asks;
  `docs/stop.md` and this file are updated with it.
- Responsive check at `2560x1440` (brief §38 asks for all three of 1280x720 / 1920x1080 / 2560x1440;
  16:9 keeps the design canvas at 1920x1080, so the check is that everything scales by 4/3 framebuffer
  px per design unit and nothing clips).
- Live verification in `runClient`: freelook smoothness, keystrokes animation, Target Info
  enter/flash/exit, Mouse Display drift/recentre, the wheel direction on every screen, and the
  cosmetics import/equip/clear path with a user-supplied PNG still need hands-on feel (the screens
  themselves are screenshot-verified with measurements).
- Known cosmetic issue, deliberately not papered over: `AetherFont.height(Size)` has an 8 px floor,
  so at the smallest GUI scales text can be a pixel taller than the row box assumes.
- Phases 7-19 above.

## Exact next task

**Commit the correction pass** (when the user asks), then **Phase 7 — Block Info transition**
(spec in `docs/stop.md`).
