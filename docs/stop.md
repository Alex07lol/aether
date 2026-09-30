# Aether — STOP / HANDOFF

Read this first if you are continuing this project. It is written to be actionable, not narrative.
Update it every time work pauses.

---

## CURRENT PHASE

**Phase 6 — Mouse Display** has not been started. Phases 1-5 are complete, and the mid-plan request
that interrupted them - *"also copy the leaf client UI exactly with colour changes of transparent
black and white"* - is **complete in code and green**, but has **not been looked at on a running
client** (see TEST STATUS).

## CURRENT OBJECTIVE

Port Soar v4 behaviour into Aether's existing module/HUD/setting systems, keep the Aether
structure, and render the whole GUI as the Leaf Client 1.8.9 composition in translucent black and
white over the live Minecraft world. No parallel systems. No dead settings. Frame-rate-independent
animation.

---

## COMPLETED WORK

### Phase 1 — audit + documentation
- `docs/progress.md` (tracker), `docs/architecture.md` (real architecture), `docs/stop.md` (this).
- Audited: module registry/settings/state machines, HUD system, all mixins, Forge bridges, the
  Leaf-rebuilt GUI, the pure `graphics/*` maths, and the decompiled **Forge 1.8.9** client
  sources that live at `build/tmp/recompileMc/sources` (that directory is the ground truth for any
  vanilla/Forge hook question — use it instead of guessing).
- Module gap analysis → **`docs/architecture.md` §15**. That table is the roadmap for Phases 5-16:
  every Soar module named in the brief is marked `HAVE` (with the Aether module id that owns it),
  `MISSING` (with the phase that implements it) or `SKIP` (with the reason). Aether registers 59
  modules; the id list was generated from the registry, not from class names.
- Found during the audit: `graphics.item_physics` and `graphics.motion_blur` exist as classes but are
  **not registered** in `GraphicsModules`, so they cannot be enabled and never appear in the UI.
  Either implement + register them (Phase 14) or delete them — they are the only unregistered
  module classes in the tree.

### Phase 2 — animation infrastructure (new package `dev/aether/animation`)
- `FrameClock` — one frame delta + frame index per rendered frame, published from
  `TickEvent.RenderTickEvent` START in `ForgeClientEventBridge`.
- `Anim` — persistent animated scalar (target/duration/easing, once-per-frame guard, `set`,
  `jumpTo`, `reverse`, `settled`, `progress`).
- `Easing` — nine easing curves.
- `AnimationMath` — `clamp01`, `lerp`, `lerpColor`, `approach`, `frameStep`.
- `src/test/java/dev/aether/animation/AnimationTest.java` — main-style self test (runs under
  `coreSelfTests`), proves frame-rate independence at 30/60/120/240 FPS and that double-update in
  one frame cannot advance the value twice.

### Phase 3 — freelook jitter (root cause proven, fixed)
Root cause: Forge posts `MouseEvent` from `Minecraft.runTick()`, and `runGameLoop()` calls
`runTick()` once **per elapsed game tick** (`for (j < timer.elapsedTicks) runTick()`), so Aether
was rotating the freelook camera at ~20 Hz against a frame-rate render. Vanilla's own look path
(`EntityRenderer.updateCameraAndRender` → `MouseHelper.mouseXYChange()`) runs per frame.
A second defect: cancelling `MouseEvent` also cancels vanilla's button/wheel handling
(`if (ForgeHooksClient.postMouseEvent()) continue;`), so attack/use/hotbar-scroll were swallowed
while freelooking.
Fix: `MouseHelperMixin` publishes the per-frame delta; freelook consumes it at the camera hook and
`MouseEvent` is no longer cancelled.

### Phase 4 — keystrokes animation
Per-key animated press/release/scale in `ForgeHudRenderer.drawKeyBox`, driven by `Anim` with
`fade_time` as the release duration, no per-frame allocation and no shared animation value.

### GUI pass — Leaf UI copied exactly (transparent black + white)
Landed mid-plan on request. The full record is `docs/GUI_REBUILD.md`; the short version:

- `scripts/leaf_assets.py` recolours Leaf's art at build time (plateau-driven `surface_mask` +
  measured `body_luminance`, rules `glass` / `white` / `shade` / `detail`). 29 textures in
  `assets/aether/leaf/**`, `NOTICE.txt` included, all packaged in the jar. **Run it with
  `--convert` after any change to the table, and look at `--sheet` - the previous rule silently
  dropped every icon and no text dump could show it.**
- `Mc189Compat.drawTextureTinted` (hand-built quad, because `Gui`'s helpers reset the vertex colour)
  and `gui/leaf/LeafArt` (asset names, `draw`, `drawHovered`; a state is a brightness).
- Every `gui/leaf/*` component and all five screens now draw Leaf's art at Leaf's coordinates.
- Palette: `ThemePalettes.mono()` is both the default theme and the *Monochrome* theme module;
  `ACCENT_ON` follows the accent (no fixed green); the HUD editor's bar states are palette tones.
- Panel rectangles were measured from the generated backdrops (`main.png` panel = design
  x 348..1571, y 175..901; `main_mod.png` = x 598..1321, y 178..901) and every control checked
  against them.

### Phase 5 — Target Info (`hud.target_info`)
- `TargetInfoModule` + 14 settings, registered in `HudModules`, `HudElement` added in
  `BuiltInModules` at (260, 164).
- `ForgeHudRenderer`: `renderTargetInfo` / `renderTargetInfoPreview` / `layoutTargetInfo` /
  `drawTargetInfo` / `targetHealthText` / `plainText`, plus a reusable `TargetLayout` holder so the
  layout never allocates and the editor measures with the same method the card draws with.
- Animations: `targetEnter` (open/close: fade + grow over `animation_time`, ease-out-cubic, and the
  last target keeps drawing while it closes), `targetDamage` (edge-triggered flash tinting the card
  towards `damage_color` for exactly `damage_flash_time`), smoothed health for the bar and the
  number (`AnimationMath.approach`, rate from `damage_flash_time`).
- Real skin: `Mc189Compat.skinLocation(entity)` returns the player's own skin, and the new
  `Mc189Compat.drawTextureRegion(...)` draws the head face (u=8,v=8) plus the hat overlay
  (u=40,v=8) of a 64x64 skin with the widget's own alpha, so the module's opacity/fade reaches the
  skin too. `Gui`'s own texture helpers cannot do that: they force the vertex colour to opaque white.
- Target source: `ForgeClientEventBridge.targetEntity()` — `objectMouseOver.entityHit` first (the
  same ray trace the block outline uses, and never the local player), then the last entity hit
  within `TARGET_MEMORY_MILLIS` (3 s), dropped on a world change or when the entity's health hits 0.
- New `Mc189Compat` accessors: `entityHit` (MCP `entityHit` / SRG `field_72308_g`), `health`
  (`getHealth` / `func_110143_aJ`), `maxHealth` (`getMaxHealth` / `func_110138_aP`), `skinLocation`
  (`getLocationSkin` / `func_110306_p` / `func_178837_g`). Each carries the MCP name for dev and the
  SRG name for production, because these are reflective lookups and reobf cannot rewrite strings.
- Health drop → damage flash is an **edge** on the entity's real health (not on the smoothed bar
  value), so one hit is one flash instead of a flash that stays pinned while the bar catches up.

---

## FILES MODIFIED / ADDED (Phase 5)

New:
- `src/main/java/dev/aether/module/impl/hud/TargetInfoModule.java`
- `src/main/java/dev/aether/hud/TargetHealthText.java` (pure health-line/fraction rules)
- `src/test/java/dev/aether/hud/TargetHealthTextTest.java`

Modified:
- `src/main/java/dev/aether/module/builtin/HudModules.java` — registers `TargetInfoModule`
- `src/main/java/dev/aether/module/builtin/BuiltInModules.java` — `hud.target_info` HUD element
- `src/main/java/dev/aether/forge189/ForgeHudRenderer.java` — target card fields, `renderTargetInfo`,
  `renderTargetInfoPreview`, `layoutTargetInfo`, `drawTargetInfo`, `targetHealthText`, `plainText`,
  the `hud.target_info` case in `getDimensions`, and the editor preview call
- `src/main/java/dev/aether/forge189/ForgeClientEventBridge.java` — `targetEntity()`,
  `rememberTarget`/`forgetTarget`, last-target memory (also called from `onAttackEntity`)
- `src/main/java/dev/aether/forge189/Mc189Compat.java` — `entityHit`, `health`, `maxHealth`,
  `skinLocation`, `drawTextureRegion`

## FILES MODIFIED / ADDED (Phases 1-4)

New:
- `src/main/java/dev/aether/animation/Easing.java`
- `src/main/java/dev/aether/animation/AnimationMath.java`
- `src/main/java/dev/aether/animation/FrameClock.java`
- `src/main/java/dev/aether/animation/Anim.java`
- `src/test/java/dev/aether/animation/AnimationTest.java`
- `src/main/java/dev/aether/forge189/mixin/MouseHelperMixin.java`
- `docs/progress.md`, `docs/stop.md` (`docs/architecture.md` is the same file as the pre-existing
  `docs/ARCHITECTURE.md` on this case-insensitive checkout, so git reports it as a modification)

Modified:
- `src/main/resources/mixins.aether.json` — `MouseHelperMixin` added to the client list
- `src/main/java/dev/aether/forge189/MixinFeatures.java` — `Mouse` holder (frame delta)
- `src/main/java/dev/aether/forge189/ForgeClientEventBridge.java` — `RenderTickEvent` frame-clock
  hook; `onMouse(MouseEvent)` deleted (no more cancellation); `onCameraSetup` now drives the camera
  from the frame delta and folds in vanilla's `invertMouse`
- `src/main/java/dev/aether/forge189/Mc189Compat.java` — `invertMouse(GameSettings)` accessor
  (MCP `invertMouse` / SRG `field_74338_d`)
- `src/main/java/dev/aether/forge189/ForgeHudRenderer.java` — per-key animated keystrokes, real
  editor preview, dead key-state maps + `blend()` helper removed
- `docs/ARCHITECTURE.md` — replaced with the real architecture (see the note in `progress.md`)

Nothing is committed. `git status` is the authority.

## FILES CURRENTLY BEING WORKED ON

None — the tree is buildable and the full verification gate is green.

Nothing has been committed. `git status` is the authority (see the note about the case-insensitive
`docs/architecture.md` path under BUILD STATUS / progress.md).

---

## MODULES PORTED

- **`hud.target_info` (Target Info)** — new, Phase 5. Skin + name + animated health bar, enter/exit
  and damage animations (Soar's `introAnimation` / `damageAnimation` / health animation).

Existing Aether modules upgraded so far: `hud.keystrokes` (Phase 4), `pvp.freelook` (Phase 3).

## SETTINGS ADDED

All under `hud.target_info` (Phase 5): `show_skin`, `show_name`, `show_health_bar`,
`show_health_text`, `health_mode` (Value / Percent / Both), `show_background`, `bar_width`
(30-140, step 2), `animation_time` (60-600 ms), `damage_flash_time` (80-1200 ms),
`background_color`, `text_color`, `bar_color`, `bar_background_color`, `damage_color`.
Every one of them is read by the renderer or by the shared layout method; none is display-only.

## SETTINGS MODIFIED

None. Keystrokes settings keep their ids, labels, defaults and ranges.

## ANIMATIONS IMPLEMENTED

- Shared `Anim`/`FrameClock`/`Easing`/`AnimationMath` layer.
- Keystrokes: per-key press progress (fast) + release fade (`fade_time`), press scale feedback,
  background↔pressed colour blend driven by the animated value.
- Target Info: open/close (fade + grow, `animation_time`), damage flash (edge-triggered,
  `damage_flash_time`, card tint towards `damage_color`), health bar/number interpolation.

## MIXINS ADDED / CHANGED

- Added: `MouseHelperMixin` → `net.minecraft.util.MouseHelper#mouseXYChange()` at `RETURN`.
- Unchanged: `EntityMixin` (rotation freeze), `EntityRendererMixin`, `ItemRendererMixin`,
  `RendererLivingEntityMixin`, `WorldMixin`.

---

## KNOWN BUGS

1. **Freelook camera collision uses the player's facing, not the camera's.**
   `EntityRenderer.orientCamera` raytraces the third-person pull-back along
   `entity.rotationYaw/rotationPitch` (`f1`/`f2`); under freelook the camera faces elsewhere, so
   the camera can clip into geometry and `d3` can pop near walls. Fix = `@Redirect` the
   `this.mc.theWorld.rayTraceBlocks(Vec3, Vec3)` call inside `orientCamera`: from the two
   endpoints reconstruct the direction and length, replace the direction with
   `FreelookView`'s yaw/pitch, and pass the corrected pair through to the real raytrace.
   Keep `require = 0`.
2. Freelook `MouseEvent`-era behaviour is gone, but the *first* frame after the key goes down can
   still apply deltas captured before activation — `ForgeClientEventBridge` clears the pending
   delta on start/stop; verify in game.

## BUILD STATUS

Green. `scripts/verify.sh` passed end to end after the GUI pass:
`compileJava` → `coreSelfTests` (29/29) → `build` → jar proof (refmap searge+notch tables, every
compiled mixin present in `mixins.aether.json`, manifest bootstrap attributes, reobf clean), and the
jar now contains the 33 `assets/aether/leaf/**` entries.
`AetherSettingsMetadataTest` reports **61 modules, 208 settings, 54 ranges, 26 choice lists**; it
validates that every new numeric setting carries its own range and every choice carries its options,
which is the guard against guessed slider bounds.

## TEST STATUS

- `coreSelfTests`: **29 test classes**, all passing, including `AnimationTest` (frame-rate
  independence at 30 vs 240 FPS, no double-step per frame, snap at duration 0, interrupt/resume) and
  `TargetHealthTextTest` (the three health modes, unknown-health handling, bar fractions).
- Verified from the refmap that `MouseHelperMixin` binds:
  `mouseXYChange -> Lnet/minecraft/util/MouseHelper;func_74374_c()V`.
- **Still needs a live `runClient`:**
  0. **The Leaf screens, first and most important:** open the GUI and check that the backdrop panel,
     the four nav tiles, the card grid and the scrollbar line up (they are all inside Leaf's own
     panel rectangles, computed not eyeballed), that the tile art shows its white glyphs (not an
     empty black rectangle - that is what a stale unconverted asset looks like), that clicking a
     card's top half toggles and its bottom half opens settings, and that nothing important was lost
     off a panel edge on the settings screens.
  1. Freelook feel (must be visibly smoother than before; attack/use/hotbar scroll must keep working
     while the key is held), at 60 and at 240 FPS.
  2. Keystrokes press/release animation; change `fade_time` and watch the release actually change.
  3. Target Info: aim at a mob (card should grow in), hit it (one red flash of `damage_flash_time`,
     bar drains smoothly), look away (card shrinks out), kill it (card must not linger), and check
     the HUD editor preview shows the real skin.

---

## CURRENT PROBLEM

Nothing blocking. The next phase has not been started.

## NEXT EXACT STEPS

1. **Phase 6 — Mouse Display** (`hud.mouse_display`), the same shape as Phase 5 — copy that file
   layout, do not invent a new one:
   1. New `dev.aether.module.impl.hud.MouseDisplayModule` (id `hud.mouse_display`, category HUD,
      `favoriteByDefault(false)`), registered in `HudModules.register`; add the `HudElement` in
      `BuiltInModules.registerHudLayout` (a free slot near the right column).
   2. Settings: `show_delta` (bool), `show_direction` (bool), `size` (number 6-24),
      `movement_speed` (number: how fast the dot travels, 40-1000), `background_color`,
      `indicator_color`, `show_background` (bool). The layout must be measured for `getDimensions`
      with the same method the widget draws with.
   3. Delta source: `MixinFeatures.Mouse.takeDeltaX()/takeDeltaY()` — **careful**: the freelook
      camera drains those in `onCameraSetup`. Either drain into a per-frame store in
      `ForgeClientEventBridge` (preferred: one drain, both consumers read the cached value) or move
      the drain into the bridge's render-tick handler. Do not call `takeDelta*` from two places.
   4. Movement: keep two `Anim`s (x and y) per axis of the *dot's offset*, or better a single
      offset vector `Anim` for its distance plus an `Anim` for its angle — whichever is implemented,
      it must be frame-rate independent, clamped to the widget's radius, and must visibly react to
      direction. `AnimationMath.approach` with a rate derived from `movement_speed` is the intended
      way (teleporting the dot is exactly what the brief forbids).
   5. Editor preview: real component (a static offset), same measuring method.
2. **Phase 7** — animate `hud.block_info` enter/exit (one `Anim`, `EASE_OUT_CUBIC`, driven by
   whether a block is looked at; keep the existing cached name/meta lookup).
3. **Phase 8** — Damage Tint: new `graphics.damage_tint` module, animated overlay intensity
   (`Anim` driving alpha), health threshold preserved as a setting.
4. Then Phases 9-19 in order (`docs/progress.md` has the full list, `docs/architecture.md` §15 has
   the module-by-module plan).
6. After **every** phase: `gradle compileJava coreSelfTests`, then update all three docs.
7. When the user says "run the game", relaunch with
   `export JAVA_HOME="$(pwd)/.gradle-dist/jdk8u504-b01"` and
   `"$(pwd)/.gradle-dist/gradle-4.10.3/bin/gradle" --no-daemon runClient --rerun-tasks`
   (add `-PaetherDebugShots=true` for screenshots), then read `build/runClient-*.log` and
   `run/logs/latest.log`.

---

## IMPORTANT ARCHITECTURAL DECISIONS

- **Aether core has no Minecraft imports.** Minecraft code lives only in
  `dev.aether.forge189.*`. Keep it that way.
- **Mixin 0.7 rejects non-private static fields on mixin classes.** All shared state goes in
  `dev.aether.forge189.MixinFeatures`, one nested holder per mixin target.
- **`MouseEvent` is tick-rate; `MouseHelper.mouseXYChange()` is frame-rate.** Anything smooth
  reads the latter.
- **Do not cancel `MouseEvent`** — Forge's guard is
  `if (ForgeHooksClient.postMouseEvent()) continue;`, so cancelling it eats vanilla input.
- **The camera hook is `EntityViewRenderEvent.CameraSetup`** in `orientCamera`; setting
  `event.yaw/pitch/roll` is the whole orientation. The pull-back distance is computed before that
  from the player's rotation.
- **Settings are self-describing** (`Setting.range()`, `Setting.choices()`); screens must never
  hard-code bounds.
- **Animations are frame-driven** through `FrameClock`/`Anim`; never step a visual animation from
  a tick handler and never call `System.nanoTime()` inside a renderer.
- Modules hold configuration; bridges/mixins/renderers hold Minecraft behaviour.

## THINGS THE NEXT AI MUST NOT UNDO

- The keystrokes settings and their ids: `show_background`, `show_clicks`, `show_movement_keys`,
  `show_spacebar`, `arrows`, `box_size`, `click_size`, `spacebar_height`, `gap`, `fade_time`,
  `text_color`, `background_color`, `pressed_color`.
- The freelook settings and their ids: `keybind`, `activation`, `sensitivity`, `invert_x`,
  `invert_y`.
- `HudElement`/`HudLayout` as the only HUD geometry system, and their `hud.<id>.*` config keys.
- The single `ForgeHudRenderer`; extend it, do not add a second HUD renderer.
- The Leaf GUI (`gui/AetherGui`, `gui/screens/*`, `gui/leaf/*`, `gui/leaf/LeafArt`) and the
  recoloured art under `assets/aether/leaf/**`. The art is GPLv3-derived Leaf art: keep
  `NOTICE.txt` with it, and regenerate through `scripts/leaf_assets.py` rather than editing PNGs by
  hand.
- The monochrome default palette (`ThemePalettes.mono()` in both `AetherTheme.defaultTheme()` and
  the Monochrome theme module). A coloured palette is a theme module, never a change to the default.
- The `MixinFeatures` indirection (non-private statics on mixins break the whole feature
  silently).
- `docs/GUI_REBUILD.md`'s decision record (single `GuiScale` converter, Leaf-exact geometry, the
  measured panel rectangles, and the reasons the palette has no second hue).
