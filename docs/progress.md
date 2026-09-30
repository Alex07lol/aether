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

**Phase 6 — Mouse Display** (Phases 1-5 complete, and the Leaf UI copy asked for mid-plan is
complete; see below). Next exact task is recorded in `docs/stop.md`.

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
- [x] Dead code: unused `Mc189Compat.hasResource` and unused `ThemePalettes.names()` removed
- [x] `compileJava`, `coreSelfTests` (29/29; registry now 61 modules) and `scripts/verify.sh`
      (build + jar proof) green
- [ ] Live confirmation in `runClient` (the screens have not been looked at on a running client)

## Phases 6-19 (planned)

- [ ] **Phase 6 — Mouse Display** (delta → clamped target → interpolated indicator)
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

- Live verification in `runClient`: freelook smoothness and keystrokes animation, Target Info
  enter/flash/exit, and - new - the ported Leaf screens themselves (nothing in the GUI has been
  looked at on a running client yet; only the assets were verified numerically).
- Phases 5-19 above.

## Exact next task

**Phase 6 — Mouse Display.** Full spec and the exact first edit are in `docs/stop.md` under
*NEXT EXACT STEPS*.
