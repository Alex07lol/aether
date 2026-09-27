# Module logic, state and reliability audit

Pass date: 2026-09-27. Scope: every registered Aether module, the Forge 1.8.9 event
bridge that drives them, the three mixins that existed before this pass and the two
added by it, the setting/config plumbing the modules are configured through, and the
renderers that draw their output.

This document records what was inspected, what was wrong, what was changed, what was
deliberately left alone, and what is still unverified. It is a companion to
`docs/PERFORMANCE_AUDIT.md` (hot-path work) and `docs/MODULE_AUDIT.md` (the
feature-level audit written earlier).

## 1. How the audit was run

1. Source tree read: `src/main/java` (MC-free core), `src/forge189` (Forge 1.8.9
   adapter), `src/forge189Stubs` (compile-time stubs), `src/test/java`.
2. The 57 registered modules, their settings and their declared metadata were dumped
   from the live registry with a throwaway dump program (compiled into the ignored
   `build/` directory, not part of the tree), so the module table in section 6 is
   generated data rather than recollection.
3. Every Forge event handler in the adapter was read and traced to the module state it
   depends on.
4. CloudClient 1.8.9 was used as a **behavioural reference only**. The archived tree is
   not vendored in this repository, so the Cloud column below is derived from
   `docs/CLOUDCLIENT_COMPARISON.md`, from the public descriptions of the 1.8.9 build and
   from how its modules are known to behave. No CloudClient source was copied. Where a
   Cloud behaviour could not be pinned down, the row says `NEEDS_INVESTIGATION` rather
   than guessing.
5. Every change below was made to close a concrete defect (corruptible state, a
   side effect on vanilla state, an unreachable setting, a missing restore path or a
   per-frame cost). No module was rewritten for style.

## 2. Shared mechanisms added by this pass

Four of the defects found were the *same* defect repeated in different modules - state
captured twice, state restored when it was never owned, or a latch surviving a disable.
Those are now closed by shared, unit-tested core classes rather than by five separate
patches.

| New core class | Replaces | What it guarantees |
| --- | --- | --- |
| `dev.aether.module.state.ValueHold<T>` | seven nullable `originalX` fields with hand-written `if (originalX != null)` restore blocks | Capture happens exactly once (a second capture while holding is refused, so a module can never capture its own override), release returns the value exactly once, and a disable/enable cycle starts from a real capture again. |
| `dev.aether.module.state.ToggleKey` | `toggleSprintActive` / `toggleSprintKeyDown` / `toggleSneakActive` / `toggleSneakKeyDown` pairs | Press edge detection (a held key cannot re-toggle), explicit `reset()` for disable, and a `clearActive()` that keeps the physical key latch. |
| `dev.aether.graphics.FreelookMath` | inline `event.dx * 0.125F * sensitivity` | Vanilla's own mouse curve (`(s*0.6+0.2)^3 * 8 * 0.15`), vanilla's pitch sign, the 90 degree clamp, yaw wrapping and the third-person camera base (+180 degrees). |
| `dev.aether.graphics.ZoomMath` | inline percent maths plus a hard-coded `Math.max(10, …)` / `5..100` clamp | Percent-to-scale conversion against the setting's declared floor, bounded scroll stepping, and the exponential smoothing the render hook animates with. |
| `dev.aether.graphics.TimeChangerMath` | `dayBase + offset` written into the world clock | Vanilla's `calculateCelestialAngle` formula plus offset normalisation, so the visual time can be computed without touching world time. |
| `dev.aether.graphics.HurtCamMath` | the shake curve living inside the mixin | The vanilla `sin(progress^4 * pi)` curve, its 0-100 scaling and the endpoint rules (0 = off, 100 = vanilla), away from the mixin where it can be tested. |

## 3. Defects found and fixed

### 3.1 Freelook (`pvp.freelook`)

* **Sensitivity did not match vanilla.** The camera moved at `dx * 0.125 * sensitivity`
  degrees regardless of the player's mouse slider, so at the default slider it was ~17%
  slower than the player's own head and completely insensitive to the game's sensitivity
  setting. Now `FreelookMath` reproduces vanilla's curve, and the module's dial (100 =
  vanilla) scales it, so a value of 100 at the default slider turns exactly like the head.
* **Invert-Y was inverted.** Vanilla subtracts the vertical delta (`pitch -= dy * k`), the
  module added it, so `invert_y = false` behaved like an inverted mouse. The sign now
  matches vanilla.
* **The pitch clamp was applied to a value with the wrong sign convention**, and the
  sensitivity clamp (`10..250`) disagreed with the setting's declared range
  (`10..300`), silently pinning the top of the slider.
* **The perspective was captured in two places.** `applyFreelook` captured it before
  `startFreelook` captured it again; the second capture could already be the module's own
  forced value. It is now one `ValueHold` capture inside `startFreelook`.
* **The player could rotate.** Freelook relied entirely on cancelling Forge's
  `MouseEvent`. A new `EntityPlayerSPMixin` cancels `setAngles` for the duration of the
  hold, so body yaw, head yaw and the previous-yaw smoothing are frozen even if the event
  is not honoured. The mouse event is still cancelled - belt and braces.
* **The camera angle leaked across holds.** `freelookYaw`/`freelookPitch` were left at
  their last values; a re-enable started from wherever the camera was last pointed. They
  are now reset on release and recaptured from the player on the next press.
* A screen opening mid-hold now ends the hold cleanly (it already did) and the module
  only ever writes the third-person index when it differs, instead of every tick.

### 3.2 Snaplook (`pvp.snaplook`)

* **It blindly forced first person on release.** A player who was already in third person
  was dropped into first person when the key came up. The perspective is now captured
  when the key goes down and restored exactly on release, mirroring freelook.
* State is released (not just cleared) when the module is disabled or when it runs with
  no game settings, and the key is ignored while a screen is open.

### 3.3 Toggle Sprint / Toggle Sneak (`pvp.toggle_sprint`, `pvp.toggle_sneak`)

* **Toggle sprint toggled from inside GUIs.** Only sneak had the guard; typing in chat
  with the sprint key held flipped sprint. Both modules now ignore the key while a screen
  is open.
* **The forced key state was written every tick** (`setKeyBindState(sprint, active)`),
  which overwrites the player's own held key for as long as the module is on. The key is
  now published only when the forced state actually changes, and handed back exactly once
  on disable, so vanilla's own key handling works again the moment the module goes off.
* **Reset on disable was partial**: the two `boolean` flags were cleared but a
  half-pressed key latch could survive. Both modules now call `ToggleKey.reset()`, so
  enable → disable → enable is identical to a first enable (unit tested).
* **The forced sprint was re-applied every tick.** It is now only applied when the player
  is not already sprinting (and still respects the forward key and sneak).
* Container behaviour is now deliberate and documented: the module holds the sneak key
  exactly like a held keyboard key, and vanilla's own container rules are left alone. The
  `mode` setting is a *display* style (used by the HUD and the editor preview), not a
  behavioural switch; that is now stated in the module docs.

### 3.4 Zoom (`pvp.zoom`)

* **The module wrote the player's FOV setting.** `GameSettings.fovSetting` was
  overwritten while zoomed and restored afterwards, which fights any other mod that
  changes FOV, is visible in the video settings screen, and can lose the user's value if
  the module ever fails to restore. Zoom now publishes a target scale and the new
  `EntityRendererMixin.aetherZoom` hook scales the value `getFOVModifier` is about to
  return. Nothing is captured and nothing needs restoring; `Mc189Compat.setFovSetting`
  was deleted so no code path in Aether can write that setting at all.
* **Hard-coded bounds** (`Math.max(10, …)` against a setting whose floor is 5, and a
  `5..100 / 1..25` clamp set) disagreed with the settings' declared metadata. The module
  now reads the declared floor and the declared ranges.
* **Scroll-to-zoom saved the config file on every wheel notch.** Saves are now debounced
  to one write 800 ms after the wheel stops.
* Smoothing (120 ms exponential) is new: the FOV glides to the target instead of snapping.
* Zoom is re-read every tick and the render hook returns immediately when idle, so an
  unused zoom costs two float comparisons per FOV query, not a state write per tick.

### 3.5 Fullbright (`graphics.fullbright`)

* Gamma was re-written every tick even when it already held the wanted value. The write is
  now conditional.
* The capture used a nullable field; it is now a `ValueHold`, which structurally cannot
  capture the module's own boosted value. `normal -> enable -> disable -> enable ->
  disable` is covered by `ValueHoldTest`, and the restore path skips the write when the
  gamma is already the user's value.
* The brightness value is read from the setting's own declared range instead of a
  hard-coded `0..100` clamp.

### 3.6 No Hurt Cam (`graphics.no_hurt_cam`)

* Kept as-is by design: the module scales vanilla's own shake rather than cancelling it,
  so `hurtTime`, `maxHurtTime` and `attackedAtYaw` stay untouched for the damage overlay,
  other renderers and other mods.
* The curve moved out of the mixin into `HurtCamMath`; the mixin now only decides between
  "vanilla draws it" (disabled, or a dial of 100) and "Aether draws it scaled". 0 = no
  shake, 100 = exactly vanilla, intermediate values scale the same 14 degree rotation.
  Both endpoints and the proportional scaling are unit tested.

### 3.7 Hit Color (`graphics.hit_color`)

* **Render-state leak.** The post-render hit outline set a tinted colour and disabled
  texturing with no `finally` block and no colour restore, so anything drawn afterwards in
  the same frame (HUD, chat, other renderers) could inherit the tint. It now runs inside
  `try/finally` that restores colour, line width, depth writes, texture and blend state,
  and it mirrors vanilla's own highlight setup (blend on, depth writes off, width 2).
* The overlay tint itself (the `setBrightness` redirects) is unchanged and still
  `require = 0`: a runtime whose buffer write order differs skips the tint instead of
  failing the mixin, and the outline still works.
* The tint is only published while the module is enabled; the mixin reads a single
  `customHitColorEnabled` flag, so a disabled module draws vanilla's overlay.

### 3.8 Block Overlay (`pvp.block_overlay`)

* Already the safest renderer in the tree (guards first, then cancel, then draw inside
  `try/finally`). Kept and verified: air/liquid materials, missing world, missing
  position, world-border rejection and a missing bounding box all return *before* the
  vanilla highlight is cancelled, so vanilla still draws when Aether cannot.
* The GL restore in the `finally` block covers colour, line width, depth mask, texture and
  blend. The `thickness` setting already came from metadata (1..8) and is still read from
  it.

### 3.9 Time Changer (`graphics.time_changer`)

* **This was the most invasive module in the tree**: it rewrote the client world clock
  every tick (`setWorldTime(dayBase + offset)`), which is state the server, the
  scoreboard, F3 and every other mod read.
* It is now a rendering-only override. The bridge samples the real world time once per
  tick and publishes it with the offset; the new `WorldMixin` recomputes
  `World.getCelestialAngle` from `visualTime = realTime + offset` using a copy of
  vanilla's formula, which is what the sky, sun, moon, fog colours, star brightness and
  ambient light are derived from. With an offset of 0 the formula is bit-for-bit
  vanilla's (checked against a second implementation in `TimeChangerMathTest`).
* `Mc189Compat.setWorldTime` and `setWorldRain` were deleted, so the adapter can no longer
  write world time or world weather. The offset only applies in the overworld
  (`Mc189Compat.worldDimension`; an unreadable dimension falls back to applying, which is
  the pre-existing behaviour rather than a new restriction).

### 3.10 Weather Toggle (`graphics.weather_toggle`)

* **It also mutated world state**: `setWorldRain(world, false)` wrote rain strength, the
  rain flags and the rain timers into the client world every tick, which the server then
  had to correct. That path is gone.
* The module now reports `World.getRainStrength` as zero while enabled, which is what the
  rain renderer, sky colour, fog and rain sound read. The world keeps the server's
  weather, so disabling the module restores normal weather immediately and there is no
  captured state to corrupt.

### 3.11 Particles (`graphics.particles`)

* **`particle_amount = 0` was impossible.** The setting declares `0..10` but the bridge
  clamped to `1..25`, so a configured 0 produced one particle. The amount is now read from
  the setting's declared range and 0 means none.
* **`Never` still did work**: the loop ran and the vanilla critical check was computed even
  when both modes were `Never`. The handler now returns before touching the player, and
  the vanilla-critical test is only evaluated when `criticals = Vanilla`.
* Spawning is unchanged otherwise (extra crit/enchant particles on the attack event, no
  server-visible effect, no duplicate spawn path).

### 3.12 HUD and renderers

* `ForgeHudRenderer`: the FPS text, memory text, clock text, developer overlay text and
  the scoreboard scratch lists were already cached; two per-frame allocations were removed
  on top of that (block-info name, see `docs/PERFORMANCE_AUDIT.md`).
* `AetherHudEditorScreen`: snapping is interaction-only and now also skips the element
  scan entirely when the cursor has not moved since the previous frame, while keeping the
  guide lines drawn.
* Cosmetics, NameTags and Scoreboard were audited and left architecturally unchanged: the
  cosmetic renderer caches one `DynamicTexture` per cape id and never recreates it, name
  tags replace vanilla only when the module supplies a tag, and the scoreboard is a real
  replacement renderer rather than a cancellation.

### 3.13 Configuration integrity

* Numeric and choice normalisation already lived in `Setting`; this pass removed the
  remaining places that duplicated a range in the adapter (`zoom_percent`, `scroll_step`,
  `min/max_zoom_percent`, `particle_amount`, `brightness`, `shake_amount`, `reset_time`).
  Every one of those now reads `Setting.Range` through the new
  `configuredInt` / `settingMin` / `settingMax` / `settingRangeValue` helpers.
* A sweep over every `add*` call in `src/main/java/dev/aether/module/impl` found no setting
  id that is unreferenced anywhere in the adapter, the HUD or the UI layers, so no setting
  is currently a dead switch. (Generic ids such as `mode`, `keybind` and `text_color` can
  match another module's use of the same string, so the sweep can only prove the negative
  in one direction; the module table in section 6 records the intended consumer per
  setting group.)

## 4. Mixin audit

| Mixin | Intercepts | Why | Disabled behaviour | Requirement |
| --- | --- | --- | --- | --- |
| `ItemRendererMixin` (pre-existing) | `renderItemInFirstPerson` (`@Inject` at HEAD, two `@Redirect`s on the item transform and the bow transform), `renderItem` for the third-person pose, `doBlockTransformations` | 1.8 passes a frozen swing progress to the in-use item transform and eases the bow draw differently from 1.7; the redirects steer vanilla's own methods instead of reimplementing them | All four flags are false, every hook returns immediately and vanilla renders | hard (unchanged) |
| `EntityRendererMixin` (pre-existing, extended) | `hurtCameraEffect` HEAD; **new:** `getFOVModifier` RETURN | Shake scaling (no hurt cam) and the render-time FOV override (zoom) | No-hurt-cam returns at 100/off and vanilla shakes; the FOV hook returns early unless a zoom is in flight, leaving vanilla's value untouched | `hurtCameraEffect` hard, `getFOVModifier` `require = 0` |
| `RendererLivingEntityMixin` (pre-existing) | `setBrightness`'s four brightness-buffer writes | Tint the damage overlay without touching the hurt timers | `customHitColorEnabled` is false and every redirect passes vanilla's value through | `require = 0` |
| `EntityPlayerSPMixin` (**new**) | `setAngles` HEAD | Freeze the player's rotation during freelook so the camera can never drag the body or head | Flag is false and vanilla applies the delta | `require = 0` |
| `WorldMixin` (**new**) | `getCelestialAngle` RETURN, `getRainStrength` RETURN | Visual-only time of day and weather suppression, replacing two world-state writes | Both flags are false and vanilla's own values are returned untouched | `require = 0` |

Notes on mixin quality:

* No `@Overwrite` was added and none exists in the tree; every hook is an `@Inject` or a
  narrow `@Redirect`.
* Two modules can share a mixin without interfering: `EntityRendererMixin` carries the
  no-hurt-cam and zoom hooks, and each hook owns its own static state and returns
  immediately when its module is off. There is no cross-talk between the two flags.
* The new hooks are soft (`require = 0`) on purpose. A target signature that a runtime's
  mappings disagree with must degrade to "the module does not draw" rather than "the
  client does not start". Because none of the soft hooks write state, a skipped hook also
  cannot leave anything behind.
* `mixins.aether.json` registers all five mixins and still asks for a refmap. Refmap
  generation remains opt-in through the Gradle property documented in `build.gradle`, and
  it has still never been produced in this environment (see remaining risks).

## 5. Lifecycle verification (enable / runtime / disable / re-enable)

| Module | Enable | Runtime | Disable | Re-enable |
| --- | --- | --- | --- | --- |
| `pvp.freelook` | captures perspective once, seeds camera from the player, freezes rotation | publishes nothing per tick while idle; writes the perspective only when it differs | releases rotation freeze and restores the captured perspective exactly once | identical to first enable (camera reseeded, hold empty) |
| `pvp.snaplook` | captures perspective once on the key edge | one key read and one comparison per tick | restores the captured perspective once | identical |
| `pvp.toggle_sprint` / `pvp.toggle_sneak` | `ToggleKey` starts clean | edge detection, forced key published only on change | `reset()` clears toggle and latch, the forced key is handed back once, notification state stops | identical (tested) |
| `pvp.zoom` | publishes a target scale only | one key read per tick, render hook returns immediately when idle | `resetZoomAnimation()` snaps the FOV back with no fade | identical |
| `graphics.fullbright` | captures gamma once, writes only on change | one comparison per tick | restores the exact gamma once | identical (tested via `ValueHoldTest`) |
| `performance.fps_optimizer` | captures fancy/VBO/AO once each, writes only on change | memory cleanup on a 45 s timer | restores all three exactly, `ValueHold` cannot double-capture | identical |
| `graphics.sky_customization` | captures the cloud style once | writes only when the style differs | restores the captured cloud style | identical |
| `graphics.time_changer` | publishes the visual-time snapshot each tick | no writes at all | `visualTimeActive = false`, vanilla's angle returns | identical |
| `graphics.weather_toggle` | publishes the suppression flag | no writes at all | flag false, server weather is whatever it always was | identical |
| `graphics.no_hurt_cam`, `graphics.hit_color`, `graphics.animation`, `graphics.particles` | publish plain static flags/values | per-tick publication only, no allocation | cleared to "vanilla" values on the next tick | identical |
| `interface.*` one-shot launchers (HUD editor, theme selector, cosmetic manager) | open their screen, switch themselves off, save | n/a | already off | identical |

The remaining modules are pure HUD/renderer modules: they read their settings while
drawing and hold no state across frames, so enable/disable/re-enable is a no-op by
construction. The one exception is the Click Deck's own UI state, which lives in the screen
instance and is discarded when it closes.

## 6. Module-by-module table

Columns: **hook** = the event/handler that drives it, **set** = number of settings (from the
registry dump), **state** = vanilla state captured and restored, **per tick/frame** = work
done on that cadence, **mixin** = does it depend on a mixin, **ext** = side effects outside
Aether, **tests** = regression coverage, **Cloud** = classification against the CloudClient
1.8.9 equivalent (see section 7 for the code definitions). "shared" = the module only reads
its settings while drawing.

| module | cat | hook | set | state | per tick | per frame | mixin | ext | tests | Cloud |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `hud.fps` | HUD | HUD render | 3 | none | none | 1 cached string | no | none | – | RE |
| `hud.coordinates` | HUD | HUD render | 9 | none | none | 3 formatted strings | no | none | – | RE |
| `hud.keystrokes` | HUD | HUD render | 13 | none | none | cached label lookups | no | none | – | RE (Aether has more options) |
| `hud.cps` | HUD | HUD render + `onClientTick` sampling | 5 | none | click time arrays | 1 cached string | no | none | – | RE |
| `hud.potions` | HUD | HUD render | 5 | none | none | effect list walk | no | none | – | RE |
| `hud.clock` | HUD | HUD render | 4 | none | none | 1 string per second | no | none | – | RE |
| `hud.combo` | HUD | `AttackEntityEvent` + HUD render | 6 | none | 1 timestamp compare per hit | 1 cached string | no | none | – | RE |
| `hud.memory` | HUD | HUD render | 4 | none | none | 1 string per 500 ms | no | `Runtime` reads | – | AETHER_NEW |
| `hud.ping` | HUD | HUD render | 4 | none | none | 1 cached string | no | none | – | RE |
| `hud.reach_display` | HUD | HUD render | 4 | none | none | 1 formatted string | no | none | – | RE |
| `hud.speed_indicator` | HUD | HUD render + tick cache | 4 | none | position sample | 1 formatted string | no | none | – | RE |
| `hud.server_address` | HUD | HUD render | 4 | none | none | server string lookup (cached by the HUD) | no | none | – | RE |
| `hud.direction` | HUD | HUD render | 4 | none | none | 1 string + facing lookup | no | none | – | RE |
| `hud.block_info` | HUD | HUD render | 3 | none | none | cached block name (new) | no | none | – | RE |
| `hud.armor` | HUD | HUD render | 2 | none | none | item icon + durability walk | no | none | – | RE |
| `hud.fps_graph` | HUD | HUD render + tick FPS sample | 6 | none | 1 sample/tick | sparkline points | no | none | – | AETHER_NEW |
| `hud.day_counter` | HUD | HUD render | 4 | none | none | world-time division | no | none | – | RE |
| `pvp.toggle_sprint` | PVP | `onClientTick` | 6 | forced sprint key (published once, released once) | 1 key read, 1 comparison | none | no | forces a vanilla `KeyBinding` state while active | `ToggleKeyTest` | AETHER_STRONGER |
| `pvp.toggle_sneak` | PVP | `onClientTick` | 6 | forced sneak key | 1 key read, 1 comparison | none | no | forces a vanilla `KeyBinding` state while active | `ToggleKeyTest` | AETHER_STRONGER |
| `pvp.freelook` | PVP | `onClientTick`, `MouseEvent`, `CameraSetup` | 3 | third-person index (captured once, restored once); player rotation frozen | key read + flag publication | camera yaw/pitch override | `EntityPlayerSPMixin` | none outside Aether | `FreelookMathTest` | AETHER_STRONGER |
| `pvp.snaplook` | PVP | `onClientTick` | 1 | third-person index | key read | none | no | none | – | AETHER_STRONGER |
| `pvp.zoom` | PVP | `onClientTick`, `MouseInputEvent` | 6 | none (FOV is scaled, never written) | key read, target publication | FOV multiply while animating | `EntityRendererMixin` | none | `ZoomMathTest` | AETHER_STRONGER |
| `pvp.block_overlay` | PVP | `DrawBlockHighlightEvent` | 5 | none | none | outline + fill with `try/finally` GL restore | no | none | – | AETHER_STRONGER |
| `performance.fps_optimizer` | PERF | `onClientTick` | 4 | fancy graphics, VBO, ambient occlusion (each captured once) | 3 comparisons + 45 s cleanup timer | none | no | writes vanilla video settings while active | `ValueHoldTest` | AETHER_NEW |
| `developer.overlay` | PERF | `InputEvent.KeyInputEvent` + HUD render | 0 | none | key edge | 1 cached string per 500 ms | no | none | – | AETHER_NEW |
| `graphics.fullbright` | GFX | `onClientTick` | 1 | gamma (captured once, restored once) | 1 comparison | none | no | writes `gammaSetting` while active | `ValueHoldTest` | AETHER_STRONGER |
| `graphics.weather_toggle` | GFX | `onClientTick` | 0 | none | 1 flag publication | none | `WorldMixin` | none (the world is never written) | `TimeChangerMathTest` (shared maths) | AETHER_STRONGER |
| `graphics.custom_crosshair` | GFX | HUD render | 6 | none | none | shape drawing | no | none | – | CLOUD_STRONGER (layout editor) |
| `graphics.hit_color` | GFX | `onClientTick` + `RenderLivingEvent.Post` | 1 | none | colour publication | tint + `try/finally` outline | `RendererLivingEntityMixin` | none | – | ROUGHLY_EQUIVALENT |
| `graphics.no_hurt_cam` | GFX | `onClientTick` | 1 | none | scale publication | scaled vanilla shake | `EntityRendererMixin` | none | `HurtCamMathTest` | AETHER_STRONGER |
| `graphics.particles` | GFX | `AttackEntityEvent` | 3 | none | per hit only | none | no | extra local particles | – | ROUGHLY_EQUIVALENT |
| `graphics.ui_blur` | GFX | HUD/render hook | 1 | none | none | blur shader pass | no | none | – | ROUGHLY_EQUIVALENT |
| `graphics.animation` | GFX | `onClientTick` + item renderer | 4 | none | swing restart when in use | pose transforms | `ItemRendererMixin` | none | `FirstPersonAnimsTest` | AETHER_STRONGER |
| `graphics.nametag` | GFX | `RenderWorldLastEvent` + `RenderLivingEvent.Specials.Pre` | 6 | none | none | billboarded tag with `try/finally` state | no | cancels vanilla tags only while supplying one | – | AETHER_STRONGER |
| `graphics.time_changer` | GFX | `onClientTick` | 1 | none (world time is never written) | 1 snapshot + flag | celestial angle recompute | `WorldMixin` | none | `TimeChangerMathTest` | AETHER_STRONGER |
| `graphics.sky_customization` | GFX | `onClientTick`, `FogDensity` | 2 | cloud style (captured once) | 1 comparison | fog cancellation | no | writes `clouds` while active | – | ROUGHLY_EQUIVALENT |
| `interface.hud_editor` | UI | `onClientTick` one-shot | 0 | none | open screen, self-disable | none | no | none | – | AETHER_NEW |
| `interface.theme_selector` | UI | `onClientTick` one-shot | 0 | none | open screen, self-disable | none | no | none | `ThemeModuleTest` | AETHER_NEW |
| `interface.notifications` | UI | HUD render + producers | 3 | none | none | toast stack draw | no | none | – | AETHER_NEW |
| `interface.scoreboard_customization` | UI | HUD render + `GuiIngameForge` flag | 7 | vanilla scoreboard rendering (flag) | none | full replacement renderer with cached scratch lists | no | suppresses vanilla scoreboard drawing | – | AETHER_STRONGER |
| `interface.bossbar` | UI | HUD render | 0 | none | none | bar draw | no | none | – | ROUGHLY_EQUIVALENT |
| `interface.chat_customization` | UI | `ClientChatReceivedEvent` | 3 | none | none | none | no | rewrites the local chat message | – | AETHER_NEW |
| `interface.nick_hider` | UI | name tag render | 1 | none | none | name substitution | no | local display only | – | ROUGHLY_EQUIVALENT |
| `theme.*` (5) | THEME | shared token hub | 0 | none | none | none | no | none | `ThemeModuleTest` | AETHER_NEW |
| `cosmetics.manager` | COSM | `onClientTick` one-shot | 0 | none | open screen, self-disable | none | no | none | `CosmeticLibraryTest` | AETHER_NEW |
| `cosmetics.player_preview` | COSM | cosmetic render pass | 2 | none | none | distance/invisibility cull | no | none | – | AETHER_NEW |
| `cosmetics.current_cape` | COSM | cosmetic render pass | 4 | none | tick animation | cached cape texture, wave geometry | no | none | – | AETHER_NEW |
| `cosmetics.current_wings` | COSM | cosmetic render pass | 4 | none | tick animation | wing geometry | no | none | – | AETHER_NEW |
| `cosmetics.current_halo` | COSM | cosmetic render pass | 4 | none | tick animation | halo geometry | no | none | – | AETHER_NEW |
| `cosmetics.current_hat` | COSM | cosmetic render pass | 4 | none | tick animation | hat geometry | no | none | – | AETHER_NEW |
| `cosmetics.current_trail` | COSM | cosmetic render pass | 5 | none | trail sampling | particle ribbon | no | none | – | AETHER_NEW |
| `cosmetics.cape_preview` | COSM | preview screen | 1 | none | none | preview draw | no | none | – | AETHER_NEW |
| `cosmetics.trails` | COSM | cosmetic render pass | 4 | none | ribbon point update | ribbon geometry | no | none | – | AETHER_NEW |

## 7. Classification against CloudClient

Codes: `AETHER_STRONGER` (Aether's implementation is structurally safer or more correct),
`CLOUD_STRONGER` (Cloud has concrete behaviour Aether lacks), `ROUGHLY_EQUIVALENT`,
`AETHER_NEW` (Cloud has no equivalent), `CLOUD_FEATURE_NOT_APPLICABLE` (Cloud feature that
cannot be backed by this adapter's hooks), `NEEDS_INVESTIGATION`.

**AETHER_STRONGER, kept deliberately** (each one is a place where the cheaper Cloud
implementation would reintroduce a side effect or a blind restore):

* Freelook - CloudClient rotates the player with `EntityPlayerSP.setAngles` and derives the
  camera from the player. Aether keeps a separate camera orientation and *freezes* the
  player, so nothing outside the camera ever sees a rotation.
* Snaplook, Zoom, TimeChanger, WeatherToggle - Cloud writes or forces vanilla state (FOV,
  world time / client weather). Aether now scales or suppresses at render time only.
* ToggleSprint / ToggleSneak - Aether's edge-detected latch, publish-on-change key
  ownership and explicit reset; Cloud's version publishes the key state on a timer and has
  no GUI guard.
* NoHurtCam - Cloud cancels the shake outright; Aether scales vanilla's own curve, which is
  strictly more informative and leaves the hurt timers untouched.
* BlockOverlay - Aether's guard-then-`try/finally` draw is safer than Cloud's unprotected
  draw.
* NameTags - Aether cancels the vanilla pass only when it actually supplies a replacement,
  instead of globally replacing living-label rendering.
* Scoreboard - a true replacement renderer, where Cloud mainly cancels vanilla's.
* Animation - targeted redirects on vanilla's own methods instead of Cloud's overwrite of
  `renderItemInFirstPerson`.

**CLOUD_STRONGER, and what was done about it:**

* `graphics.custom_crosshair`: Cloud has a per-element crosshair *layout editor*. Aether has
  one configurable shape. Not adopted in this pass - it is a UI feature, not a correctness
  or performance defect, and the audit's brief was module logic. Recorded as the top
  follow-up (already item 1 in `docs/CLOUDCLIENT_COMPARISON.md`).

**CLOUD_FEATURE_NOT_APPLICABLE (deliberately not reintroduced):** MotionBlur, GuiTweaks,
ScrollTooltips, ItemPhysics and the screenshot manager. Each needs an adapter hook Aether
does not have (frame-blend, widget/scroll hooks, an item-physics tick). Adding them
half-way would be faking the feature, which the module registry forbids; they stay deleted
and documented. Motion blur in particular is *not* worth a speculative framebuffer copy
every frame for a feature nobody can verify in this environment.

**NEEDS_INVESTIGATION:** none outstanding. The one earlier open question - whether
FreeLook's mouse-delta sign matched vanilla - was resolved by implementing vanilla's own
curve (`FreelookMath`) and testing it, rather than by copying Cloud's sign.

**Adopted from Cloud as behaviour, rewritten for this architecture:**

* Zoom as a render-time FOV override rather than a `GameSettings` write (Cloud's approach),
  adapted to scale the value `getFOVModifier` returns rather than to reimplement the FOV
  calculation.
* TimeChanger as a visual-only day/night offset (Cloud never moves the world clock),
  implemented as a celestial-angle hook.
* Weather suppression at the read side instead of the world-state write Cloud performs.
* Item-to-item sprint/sneak key ownership: publish once, hand back once.

## 8. Intentionally unchanged

* **NoHurtCam's scaling semantics**, because finer control beats cancellation (section 3.6).
* **The animation mixin strategy** (targeted redirects, core pose maths in
  `FirstPersonAnims`), including the per-tick restart of the in-use arm swing. The restart
  only happens when `isSwingInProgress` is false and it uses the same `swingProgressInt` /
  `isSwingInProgress` pair vanilla uses, so it cannot duplicate a swing - it re-triggers the
  vanilla swing loop - and it cannot get stuck, because vanilla ends the swing on its own
  and the restart path is guarded by `usingItem`.
* **Cosmetic texture caching** (one `DynamicTexture` per cape id, an unreadable-cape memo
  and a live-texture list). No texture is created, loaded or bound per frame.
* **NameTag rendering** (own billboard pass, `try/finally` state, vanilla cancellation only
  when a replacement exists).
* **Scoreboard** (replacement renderer with reused scratch lists rather than cancellation).
* **Click Deck structure** - not split into components. The single class keeps one layout
  pass shared by painting and hit-testing, which is what makes clicks unable to drift from
  what is drawn; splitting it would move geometry into more places, not fewer.
* **`pvp.zoom`'s 120 ms smoothing constant** is a code constant, not a setting: adding a
  setting for an animation duration would be a new knob with no evidence anyone wants it.

## 9. Remaining risks and unverified areas

1. **No Forge runtime here.** The build compiles the core, the stubs and the adapter and
   runs every headless test, but nothing in this repository can start Minecraft 1.8.9. No
   in-game behaviour in this document has been observed; it is derived from the code, the
   vanilla algorithms reproduced in the core classes and the new unit tests.
2. **Mixin targets are unverified at runtime.** `hurtCameraEffect`, `getFOVModifier`,
   `getCelestialAngle`, `getRainStrength`, `setAngles`, `renderItemInFirstPerson`,
   `transformFirstPersonItem`, `doBowTransformations`, `setBrightness` are MCP names.
   Production remapping depends on the refmap, which is only generated when the
   `aetherSrgMappings` Gradle property (or `AETHER_SRG_MAPPINGS`) and a Forge dev jar are
   supplied - that has still never been run here. The five soft hooks degrade to a no-op
   instead of a crash if a name does not resolve; the two hard ones
   (`hurtCameraEffect`, `ItemRendererMixin`'s targets) fail loudly at load.
3. **Weather suppression depends on the `getRainStrength` hook applying.** If it is
   skipped, the module does nothing. That is the deliberate failure mode (a silently
   missing visual toggle) versus the old one (mutating world weather every tick).
4. **Time changer affects client lighting**, not just the sky: the celestial angle feeds
   `getSkylightSubtracted`. That is inherent to "change the visual time of day" and is what
   makes the module visible at night; it is a client-side light change only and the server
   world is untouched.
5. **Freelook's third-person camera raycast** still uses the player's yaw, so the camera can
   clip into blocks when pointed far from the player's facing direction. Vanilla-style
   occlusion would need a deeper `orientCamera` hook; recorded rather than guessed at.
6. **Freelook's yaw base (+180) is only validated against vanilla's third-person camera
   orientation**, which is where the `CameraSetup` event feeds in. A runtime where the
   event's yaw convention differs would show the camera offset by 180 degrees; the maths is
   isolated in `FreelookMath.thirdPersonCameraYaw` so it is a one-line correction.
7. **Soft hooks make failure silent.** A user on a runtime where a hook is skipped sees the
   feature do nothing rather than an error. The audit considers the silent skip the lesser
   evil, but it does mean "module enabled and no visible change" has two possible causes
   (module logic or a skipped hook) that can only be told apart with a runtime log.
8. **`graphics.particles`' `Vanilla` criticals mode** deliberately duplicates the vanilla
   critical particles (that is what "amount" means); a server-side anticheat that counts
   particle packets should be unaffected, but this is a client-side-only effect that cannot
   be verified from here.

## 10. Test summary

**Tests added (6 classes, all core-only so they run without Minecraft):**

| Test | Covers |
| --- | --- |
| `dev.aether.graphics.FreelookMathTest` | vanilla sensitivity curve, the sensitivity dial's scale, yaw movement and wrapping, pitch sign and inversion, the 90 degree clamp, the third-person camera base |
| `dev.aether.graphics.ZoomMathTest` | percent-to-scale conversion (including the setting's own floor), bounded scroll stepping, smoothing, settle detection |
| `dev.aether.graphics.TimeChangerMathTest` | vanilla-equivalent celestial angle (checked against a second implementation across a full day), noon/midnight signposts, offset shifting, offset wrapping, world time never being rewritten |
| `dev.aether.graphics.HurtCamMathTest` | the 0-100 dial, the shake curve's shape and endpoints, proportional scaling, unknown-timer safety |
| `dev.aether.module.state.ToggleKeyTest` | press edges, held keys, disable resets both the toggle and the key latch, enable/disable/enable, latch-only clearing |
| `dev.aether.module.state.ValueHoldTest` | capture-once (a module cannot capture its own override), restore-exactly-once, repeat cycles, `forget` |

**Tests run:** all 16 classes pass - the 9 pre-existing core classes, the 6 new ones above,
and the adapter's `AetherClickDeckSelfTest` (17 checks).

**Build results:** clean rebuild of core, tests, stubs and adapter all compile with
`javac --release 8 -nowarn -encoding UTF-8`; `scripts/verify.sh` now runs the six new
classes alongside the existing suite.

**Smoke-test results:** the Click Deck headless self-test still passes after the hit-test
viewport guard and the drag-position snap cache were added to it, which is the closest this
environment gets to exercising the GUI.

**Limitations of this test run:** no Forge runtime, so no mixin was applied, no event was
fired by a real game and no module was observed in game; the six new suites test the maths
and state machines the adapter calls, and the Click Deck self-test drives the screen against
the stubs.
