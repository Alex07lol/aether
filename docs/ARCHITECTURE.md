# Aether — Architecture (as implemented)

Everything in this document was read out of the source tree, not from a design intention.
If the code changes in a way that contradicts this file, this file is wrong and must be updated
in the same commit.

Target: **Minecraft 1.8.9, Forge 11.15.1.2318, Java 8, ForgeGradle 2.1 + MixinGradle 0.6 + Mixin
0.7.11**. One source set (`src/main/java`), which holds both the platform-neutral core
(`dev.aether.*`) and the Forge adapter (`dev.aether.forge189.*`).

---

## 1. Layering

```
dev.aether.forge189.*        Forge adapter: mod container, event bridges, mixins, renderers, screens
        |
dev.aether.*  (core)         client, modules, settings, HUD layout, theme, config, cosmetics,
                             pure maths (graphics/*), pure animation maths (animation/*)
```

The core never imports Minecraft. Only `dev.aether.forge189.*` does.
`Mc189Compat` is the single reflection/`Object` boundary used by bridges that hold vanilla types
loosely; mixins use real Minecraft types because Mixin needs exact descriptors.

## 2. Entry points

| Class | Role |
| --- | --- |
| `dev.aether.forge189.AetherForgeMod` | `@Mod` container. Builds the `AetherClient`, registers the bridges on `MinecraftForge.EVENT_BUS`, owns the config save path |
| `dev.aether.forge189.mixin.AetherMixinPlugin` | `IFMLLoadingPlugin` + Mixin bootstrap (`MixinBootstrap.init()`, `Mixins.addConfiguration("mixins.aether.json")`) |
| `dev.aether.gui.AetherGui` | The only place that maps a `GuiSection` to a screen. Screens are never constructed directly by bridges |

`AetherClient` is the composition root and owns every piece of long-lived state:
`EventBus`, `ModuleRegistry`, `HudLayout`, `JsonConfigStore`, `CosmeticLibrary`,
`ClientPreferences`, `ProfileStore`, `WaypointManager`, `ScreenshotManager`, `AetherTheme`.

Config lifecycle: `AetherClient.start()` loads one `ConfigDocument` and fans it out to every
store; `AetherClient.save()` merges every store back into one document and writes it.
`System.exit` is never the save path — the container saves on world unload and on shutdown.

## 3. Module system

- `ClientModule` (interface) — `metadata()`, `state()`, `enable()`, `disable()`, `settings()`.
  Nested `ModuleState {ENABLED, DISABLED}`, `ModuleCategory {GENERAL, PERFORMANCE, GRAPHICS,
  RENDER, INTERFACE, MOVEMENT, AUDIO, HUD, PVP, COSMETICS, ACCESSIBILITY, THEMES}` and
  `ModuleMetadata` (+ builder: id, name, category, description, favoriteByDefault, group).
- `AbstractModule` — the base every real module extends. Holds the state, the settings list and
  the fluent setting factories: `addBool`, `addNumber(id,label,default)`,
  `addNumber(id,label,default,min,max,step)`, `addText`, `addChoice(id,label,default)`,
  `addChoice(id,label,default,options...)`, `addColor`, `addKeybind`. `onEnable`/`onDisable` are
  the lifecycle hooks.
- `ModuleRegistry` — insertion-ordered `LinkedHashMap<String, ClientModule>`.
  `register` runs `FairPlayPolicy.validate` and rejects duplicate ids. `setEnabled` enforces
  mutual exclusion for modules that share a `group` (that is how exactly one theme can be
  active). `toConfig`/`applyConfig` persist `module.<id>.enabled` and
  `module.<id>.setting.<settingId>`.
- `builtin/*` — the registration lists (`HudModules`, `GraphicsModules`, `PvpModules`,
  `InterfaceModules`, `PerformanceModules`, `CosmeticModules`, `ThemeModules`) called from
  `BuiltInModules.registerAll`.

**Lifecycle of a module activation:** `registry.setEnabled(id, true)` → group exclusivity →
`module.enable()` → `onEnable()`. Nothing polls module state to decide whether to enable; the
Forge bridges poll state every tick/frame to decide whether to *apply* it, which keeps the
runtime behaviour idempotent and makes "disable" mean "stop applying", never "undo a one-off".

### Module state machines (`module/state/*`)

- `ActivationLatch` + `ActivationMode` (`HOLD`/`TOGGLE`) — one implementation used by every
  hold-or-toggle key (freelook, snaplook).
- `ForceKeyMachine` — owns a vanilla keybinding on behalf of toggle-sprint / toggle-sneak:
  edge detection, publish-only-on-change, clean reset.
- `ValueHold<T>` — capture-once / restore-once for a vanilla value the client overrides
  (gamma, fancy graphics, VBO, ambient occlusion, clouds, framerate, perspective).
- `ToggleKey` — a keybind that flips a module.

## 4. Settings

`dev.aether.module.setting.Setting<T>` is final and self-describing:

- `id`, `label`, `type` (`BOOLEAN, NUMBER, TEXT, COLOR, KEYBIND, CHOICE`), `defaultValue`, `value`
- optional `Range(min, max, step)` with `clamp`/`snap` — **the slider bounds live on the setting**,
  so no screen keeps a range table
- optional `choices` list for `CHOICE`
- `setValue` normalises (numbers clamp into the range, choice values fall back to option 0)
- `reset()`, `serializeValue()`, `restoreValue(String)`

Screens read `range()`/`choices()` and never invent bounds. This is the rule that keeps
"no dead settings" enforceable: a setting with a range renders a slider that writes back into the
same object every consumer reads.

## 5. HUD system

| Class | Owns |
| --- | --- |
| `dev.aether.hud.HudElement` | `id`, `x`, `y`, `scale` (0.25-4), `opacity` (0-1), `layer` |
| `dev.aether.hud.HudLayout` | the element map (`add`/`get`/`move`/`scale`/`opacity`/`layer`/`elements`/`renderOrder`), grid snapping, persistence under `hud.<id>.*` |
| `forge189.ForgeHudRenderer` | draws every real HUD module and the HUD-editor previews; reads `HudLayout` for position/scale/opacity |
| `forge189.ForgeHudEventBridge` | publishes `HudRenderEvent`, calls the renderer from `RenderGameOverlayEvent.Post(TEXT)`, suppresses vanilla scoreboard/bossbar/crosshair when Aether draws them |

There is exactly one HUD coordinate/layout system (`HudElement`/`HudLayout`) and exactly one HUD
renderer. New HUD modules must: register a `HudElement` in `BuiltInModules`, render in
`ForgeHudRenderer`, and add their preview to the editor branch
(`renderHudEditorPreview`/the editor switch at the bottom of the renderer).

## 6. Forge event bridges

| Class | Subscribes to | Responsibility |
| --- | --- | --- |
| `ForgeClientEventBridge` | `ClientTickEvent`, `MouseEvent`, `InputEvent.KeyInputEvent`, `MouseInputEvent`, `CameraSetup`, `FogDensity`, `AttackEntityEvent`, `DrawBlockHighlightEvent`, `RenderWorldLastEvent`, `RenderGameOverlayEvent.Pre`, `RenderLivingEvent.Specials.Pre`, `RenderLivingEvent.Post`, `ClientChatReceivedEvent` | Publishes client state into `MixinFeatures`, implements freelook/zoom/snaplook/toggle-sprint/toggle-sneak/combo/hit-colour/block-overlay/weather/time/fullbright/FPS-limiter/FPS-optimizer/one-shot screen openers |
| `ForgeHudEventBridge` | `RenderGameOverlayEvent` | HUD render pass (above) |
| `ForgeGuiEventBridge` | `GuiOpenEvent` | Replaces vanilla `GuiMainMenu` with `AetherMainMenuScreen` |
| `ForgeKeyBindings` | — | Creates the Forge `KeyBinding` objects (developer overlay, mod menu) |

**The `MixinFeatures` rule:** a Mixin 0.7 class may not declare a non-private static field
(`InvalidMixinException`), and the first real `runClient` proved every Aether mixin was being
silently skipped for that reason. All state shared between the bridges and the mixins therefore
lives in `dev.aether.forge189.MixinFeatures`, one nested holder per mixin target
(`Entity`, `EntityRenderer`, `ItemRenderer`, `RendererLivingEntity`, `World`, `Mouse`).
Never put a static field on a mixin class.

## 7. Mixins

Config: `src/main/resources/mixins.aether.json`, package `dev.aether.forge189.mixin`,
`defaultRequire: 0` (a mapping mismatch skips the mixin instead of killing the client).

| Mixin | Target | Purpose |
| --- | --- | --- |
| `EntityMixin` | `net.minecraft.entity.Entity#setAngles(FF)` | Cancels mouse-driven rotation for the local player while freelook holds the camera |
| `EntityRendererMixin` | `EntityRenderer#hurtCameraEffect`, `#getFOVModifier` | Scaled hurt shake; FOV multiply for zoom (published target + animated current) |
| `ItemRendererMixin` | `ItemRenderer#renderItem` | 1.7-style first-person animation poses |
| `RendererLivingEntityMixin` | `RendererLivingEntity` | Custom hit-flash tint |
| `WorldMixin` | `World#getRainStrength`, `#getThunderStrength`, time | Render-only weather/time overrides |
| `MouseHelperMixin` | `net.minecraft.util.MouseHelper#mouseXYChange()` | Publishes the **per-frame** mouse delta (see §9) |

`EntityPlayerSPMixin` is a private marker class with no injections; it exists so the abandoned
class name is not silently reused.

## 8. Rendering utilities

- `Mc189Compat` — the compat/primitive layer: `drawRect`, `drawRoundedRectangle`,
  `drawStringWithShadow`, `pushScissor`/`popScissor` (GL scissor, scale aware),
  `guiScaleWidth/Height`, `drawTexture(path,...)`, plus every reflection accessor
  (`keyDown`, `mouseSensitivity`, `rotationYaw`, `gameSettings`, ...).
- `AetherUi` — the theme-token hub and drawing helpers for the screens
  (`drawRoundRect`, `outline`, `lerpColor`, `withAlpha`, `textSmooth`, `centeredSmooth`).
- `dev.aether.graphics.*` — pure, testable maths: `FreelookMath`, `FreelookView`, `ZoomMath`,
  `HurtCamMath`, `FirstPersonAnims`, `TimeChangerMath`, `WeatherValues`.
- `dev.aether.animation.*` — the shared frame-clock-driven animation layer (see §10).
- `forge189.font.*` — `AetherFontManager`/`GlyphPageFontRenderer` for the client UI font.

## 9. Camera and input path (the part that is easy to get wrong in 1.8.9)

Proven from the decompiled Forge sources (`build/tmp/recompileMc/sources`):

1. `Minecraft.runGameLoop()` calls `runTick()` **once per elapsed game tick**
   (`for (int j = 0; j < this.timer.elapsedTicks; ++j) this.runTick();`) and then
   `entityRenderer.updateCameraAndRender(...)` **once per frame**.
2. Forge posts `MouseEvent` from inside `runTick()`'s `while (Mouse.next())` loop, so
   `MouseEvent` fires at **tick rate**, and cancelling it also cancels vanilla's button/wheel
   handling for that event (`if (ForgeHooksClient.postMouseEvent()) continue;`).
3. Vanilla's real look path is per frame: `updateCameraAndRender` →
   `mouseHelper.mouseXYChange()` (the only call site, `MouseHelper` is
   `net.minecraft.util.MouseHelper`, `deltaX`/`deltaY` are **public**) → `thePlayer.setAngles`.
4. The camera orientation is applied in `EntityRenderer.orientCamera` from the
   `EntityViewRenderEvent.CameraSetup` event (`event.roll`, `event.pitch`, `event.yaw`).
   The third-person pull-back distance is raytraced along `entity.rotationYaw/rotationPitch`.

Consequences that Aether follows:

- Any camera/input feature that must be smooth reads **`MouseHelper.mouseXYChange()`**, never
  `MouseEvent`. `MouseHelperMixin` publishes `MixinFeatures.Mouse.frameDeltaX/frameDeltaY`.
- Freelook never writes the player's rotation. It publishes its own yaw/pitch and hands them to
  `CameraSetup`, and `EntityMixin` freezes `setAngles` for the local player as the guard.

## 10. Animation system

`dev.aether.animation` (platform-neutral, unit tested without Minecraft):

- `FrameClock` — `beginFrame()` publishes `deltaMillis()` and `frameIndex()`. Called from
  `TickEvent.RenderTickEvent` START, which FML fires once per rendered frame. Animations never
  call `System.nanoTime()` themselves, so 30/60/120/240 FPS all advance by wall-clock time.
- `Anim` — a persistent animated scalar: `target`, `durationMillis`, `Easing`. `update()` advances
  at most once per frame (guarded by the frame index), `set(value)` snaps, `jumpTo`, `reverse`,
  `settled()`, `progress()`, `value()`. Instances are fields, never allocated per frame.
- `Easing` — `LINEAR`, `EASE_IN_QUAD`, `EASE_OUT_QUAD`, `EASE_OUT_CUBIC`, `EASE_OUT_QUART`,
  `EASE_IN_OUT_CUBIC`, `EASE_OUT_EXPO`, `EASE_OUT_BACK`, `EASE_IN_BACK`.
- `AnimationMath` — `clamp01`, `lerp`, `lerpColor(from,to,t)`, `approach(current,target,dt,rate)`,
  `frameStep(...)`.

Rule: module code must not implement its own time stepping. If a value has to move over time, it
is an `Anim`. This is what keeps "Animation / Speed / Fade Time" settings honest — the setting
drives `Anim.setDuration(...)`, which is the same object the renderer reads.

## 11. UI screens and components

A copy of Leaf Client 1.8.9's screens - its art, its coordinates, its interaction split - over
Aether's registry (see `docs/GUI_REBUILD.md` for the decision record, the art pipeline and the
short list of things that stay Aether's).

- Shell: `dev.aether.gui.screens.AetherGuiScreen` (design-space canvas via `GuiScale`, Leaf's
  backdrop art per screen, the navigation row, render/mouse lifecycle). The row is Leaf's four
  rectangles plus Aether's own fifth tile: `GuiSection`'s **declaration order is the navigation
  order** and pairs with `NAV_X = {430, 650, 860, 1100, 1320}` - Leaf's modules / cosmetics / HUD
  (`location`) / settings keep its x positions and the Themes tile takes the free middle slot.
  Reordering the enum moves the tabs, so the enum and that list move together.
- Wheel handling once, in the shell: `scrollDelta(-wheel / 24)` normalises vanilla's sign (positive =
  forward/down) and applies `preference.invert_scroll`, so screens read one sign and never negate it
  again. That replaced a double negation that ran every paged list backwards.
- Sections: `AetherModScreen` (modules), `AetherModuleSettingsScreen` (one module, Leaf's
  `ModDetailSettings` role), `AetherCosmeticScreen`, `AetherThemesScreen` (the palette picker -
  Aether's own destination, built in the Cosmetics idiom from the registry's `ThemeModule`s),
  `AetherHudEditorScreen`, `AetherClientSettingsScreen`, `AetherMainMenuScreen`.
- Components: `dev.aether.gui.leaf.*` (`ModuleCard`, `NavButton`, `PageBar`, `LeafToggle`,
  `LeafBar`, `SelectButton`, `LeafTextBox`, `ColorChart`, `CosmeticEntry`) on
  `dev.aether.gui.core.UiComponent`; every one of them draws Leaf art through `LeafArt`, so a state
  (enabled, hovered, dimmed, selected) is a brightness of one texture.
- Art: `assets/aether/leaf/**` = Leaf's textures recoloured to translucent black + white by
  `scripts/leaf_assets.py` (build-time, Pillow; the client never processes images);
  `Mc189Compat.drawTextureTinted` is the primitive, `NOTICE.txt` the attribution. Thirty textures
  today, all rebuildable from the Leaf checkout: `SOURCE_NAMES` maps the outputs Leaf has no art for
  (the Themes tab) onto the source it is cut from, and the `detail` rule's optional alpha gain makes
  a translucent original's white detail solid where a row of tiles has to read evenly.
- `dev.aether.gui.preview.PlayerPreview` renders the real player model for cosmetics.
- `dev.aether.gui.AetherFont` — semantic text roles (TITLE/SECTION/BODY/SMALL/CAPTION) over
  `AetherFontManager`.
- Theme: `dev.aether.theme.*` (`AetherTheme`, `ThemePalette`, `ThemePalettes`, `ThemeModule`)
  and the `AetherUi` tokens derived from the active theme. The default palette is
  `ThemePalettes.mono()` — translucent black and white — and is also the *Monochrome* theme module,
  so the default and the module share one palette; `ACCENT_ON` follows the accent, because on/off is
  carried by the art's own shape rather than by a fixed hue.

## 12. Extension points (the recipes)

**New module:** write `XModule extends AbstractModule`, add settings in the constructor, register
it in the matching `builtin/*Modules` list, implement the runtime behaviour in the bridge/renderer
that owns that concern (never inside the module — modules hold configuration, not Minecraft code),
and make its settings authoritative there.

**New HUD element:** register a `HudElement` id in `BuiltInModules` (`hudLayout.add(id, x, y)`),
add a `renderX` method in `ForgeHudRenderer` reading the element's geometry, and add it to the
HUD-editor preview branch so the editor shows the real component.

**New setting:** `addNumber(...).range(min,max,step)`, `addChoice(...).choices(...)`, or the other
`addX` helpers. Never hard-code its bounds anywhere else; the screen reads `range()`/`choices()`.

**New animation:** add an `Anim` field where the state lives (renderer/bridge), set its
`durationMillis` from the module setting, set the target from the runtime event, and read
`anim.value()` while drawing. Do not step it from a tick handler when it drives rendering.

## 13. Data / render flow

```
Forge event  ->  bridge (ForgeClientEventBridge / ForgeHudEventBridge / mixin)
             ->  MixinFeatures  (cross-mixin state)
             ->  renderer (ForgeHudRenderer / mixin) reads module settings + Anim values
             ->  Mc189Compat / AetherUi draw calls
```

Configuration flow:

```
screen interaction -> Setting.setValue/normalize -> registry.toConfig -> JsonConfigStore (file)
                                                        ^
AetherClient.start() -> ConfigDocument -> module.applyConfig / hudLayout.applyConfig -> Setting.restoreValue
```

## 13b. Design rules (carried over from the previous `ARCHITECTURE.md`)

The file you are reading replaces the previous 40-line `ARCHITECTURE.md`. Its aspirational parts
were removed because they described layers this repository does not contain (a separate
`forge-1.7.10` adapter, a `launcher`, a `cloud` layer). Its rules that are still true are kept
here, because they are enforced by code:

- **The core has no Minecraft dependency** and no external libraries — it must keep compiling with
  plain Java 8. Verified: only `forge189.*` imports `net.minecraft.*`.
- **Adapters do not own product rules.** A Forge keybind calls into the registry; the registry
  decides. Same for the HUD renderer: it reads settings, it does not decide policy.
- **Fair-play boundary.** Anything that automates combat, changes reach or velocity, reveals hidden
  information or manipulates packets belongs outside the client. This is enforced, not documented:
  `ModuleRegistry.register` calls `FairPlayPolicy.validate(metadata)` and rejects prohibited ids
  and names before a module can exist. Any new module ported from Soar must survive that check —
  if it cannot, it does not ship.
- **Data flow** (unchanged, and still accurate): platform event → adapter → core event/state →
  module or renderer → config save → UI reads the registry.

## 15. Soar v4 → Aether gap analysis (the roadmap for Phases 5-16)

Aether currently registers **59 modules** (the authoritative list was produced by grepping the
registry ids, not by reading module names). This table maps every Soar v4 module named in the brief
to that list.

Status legend: **HAVE** = an Aether module already owns the feature (upgrade it, never duplicate);
**MISSING** = implement as an Aether-native module (phase given); **SKIP** = deliberately not
ported, with the reason.

### HUD

| Soar | Aether | Status / action |
| --- | --- | --- |
| ArmorStatusMod | `hud.armor` | HAVE — Phase 17: real armour item icons, held item, mode, endurance/damage |
| BossbarMod | `interface.bossbar` | HAVE |
| ComboCounterMod | `hud.combo` | HAVE |
| CompassMod | `hud.direction` | HAVE — Phase 17: real compass strip/degree readout |
| CoordsMod | `hud.coordinates` | HAVE |
| CPSDisplayMod | `hud.cps` | HAVE |
| DayCounterMod | `hud.day_counter` | HAVE |
| FPSDisplayMod | `hud.fps` | HAVE |
| MemoryUsageMod | `hud.memory` | HAVE |
| PingDisplayMod | `hud.ping` | HAVE |
| PotionStatusMod | `hud.potions` | HAVE — Phase 22 spec: real potion icons + compact/detailed modes |
| ServerIPDisplayMod | `hud.server_address` | HAVE |
| SpeedometerMod | `hud.speed_indicator` | HAVE |
| TimeDisplayMod | `hud.clock` | HAVE |
| KeystrokesMod | `hud.keystrokes` | HAVE — Phase 4 done (per-key animation) |
| TargetInfoMod | `hud.target_info` | HAVE — Phase 5 (skin + name + animated health bar, enter/exit and damage animations) |
| MouseDisplayMod | — | **MISSING → Phase 6** |
| HealthDisplayMod | — | MISSING → Phase 13 |
| HorseStatsMod | — | MISSING → Phase 13 |
| SaturationMod | — | MISSING → Phase 13 |
| PotionCounterMod | — | MISSING → Phase 13 (count only; `hud.potions` covers the list) |
| SessionInfoMod | — | MISSING → Phase 13 |
| StopwatchMod | — | MISSING → Phase 13 |
| WeatherDisplayMod | — | MISSING → Phase 13 |
| NameDisplayMod | — | MISSING → Phase 13 |
| ImageDisplayMod | — | MISSING → Phase 13 (needs a texture/framebuffer pipeline) |
| InventoryDisplayMod | — | MISSING → Phase 13 (inventory grid on the HUD) |
| MusicInfoMod | — | MISSING → Phase 13 (reads `MusicTicker` state) |
| PlayerDisplayMod | `cosmetics.player_preview` (screen only) | PARTIAL → Phase 13 gives the HUD element |
| MinimapMod | — | MISSING → Phase 13, **largest single item**: needs chunk→texture colouring and its own framebuffer. Budget it separately |
| RearviewMod | — | MISSING → Phase 13, second-largest: render-to-texture of the world from a second camera. High regression risk |
| ClientInfoMod | — | SKIP-able / fold into `developer.overlay` |

### Render / visual

| Soar | Aether | Status / action |
| --- | --- | --- |
| BlockInfoMod | `hud.block_info` | HAVE — Phase 7 adds the appearance transition |
| BlockOverlayMod | `pvp.block_overlay` | HAVE |
| CrosshairMod | `graphics.custom_crosshair` | HAVE |
| FullbrightMod | `graphics.fullbright` | HAVE |
| HitColorMod | `graphics.hit_color` | HAVE |
| MenuBlurMod | `graphics.ui_blur` | HAVE |
| MinimalDamageShakeMod | `graphics.no_hurt_cam` | HAVE |
| NametagMod | `graphics.nametag` | HAVE |
| NameProtectMod | `interface.nick_hider` | HAVE |
| OldAnimationsMod | `graphics.animation` | HAVE — Phase 25 spec adds armour damage / health / sneak once the poses are verified |
| OverlayEditorMod | `interface.hud_editor` | HAVE |
| ParticlesMod | `graphics.particles` | HAVE |
| ReachDisplayMod | `hud.reach_display` | HAVE |
| ScoreboardMod | `interface.scoreboard_customization` | HAVE |
| ScreenshotViewerMod | `performance.screenshot` | HAVE (capture) — Phase 14 adds the viewer screen |
| TimeChangerMod | `graphics.time_changer` | HAVE |
| WeatherChangerMod | `graphics.weather_toggle` | HAVE |
| WingsMod | `cosmetics.current_wings` | HAVE |
| ChatMod | `interface.chat_customization` | HAVE (timestamps) — Phase 14 can extend |
| DamageTintMod | — | **MISSING → Phase 8** |
| HotbarMod | — | **MISSING → Phase 10** |
| InventoryMod | — | **MISSING → Phase 11** |
| ChunkBordersMod | — | MISSING → Phase 14 |
| ClearGlassMod / ClearWaterMod | — | MISSING → Phase 14 |
| GlintColorMod | — | MISSING → Phase 14 |
| HitboxMod | — | MISSING → Phase 14 |
| MinimalBobbingMod | — | MISSING → Phase 14 |
| TNTTimerMod | — | MISSING → Phase 14 |
| UHCOverlayMod | — | MISSING → Phase 14 |
| TabEditorMod | — | MISSING → Phase 14 |
| BloodParticlesMod / DamageParticleMod | — | MISSING → Phase 14 |
| ReachCirclesMod | — | MISSING → Phase 14 |
| PackDisplayMod | — | MISSING → Phase 14 |
| BreadcrumbsMod | `waypoint.WaypointManager` (core) | MISSING as a renderer → Phase 14 can build on the existing waypoint store |
| ItemInfoMod | — | MISSING → Phase 14 |
| ItemPhysicsMod | `graphics.item_physics` (class exists, **not registered**) | Phase 14: register with real behaviour or delete |
| MotionBlurMod | `graphics.motion_blur` (class exists, **not registered**) | same as above |
| FovModifierMod | — | MISSING → Phase 15 |

### Player / PvP / utility

| Soar | Aether | Status / action |
| --- | --- | --- |
| FreelookMod | `pvp.freelook` | HAVE — Phase 3 fixed the jitter |
| ZoomMod | `pvp.zoom` | HAVE — Phase 9 smooths the FOV glide |
| SneakMod | `pvp.toggle_sneak` | HAVE |
| SprintMod | `pvp.toggle_sprint` | HAVE |
| TaplookMod | `pvp.snaplook` (partial) | HAVE-ish: snaplook holds the camera; “tap to peek” is a mode → Phase 15 |
| HitDelayFixMod | — | MISSING → Phase 15 |
| RawInputMod | — | MISSING → Phase 15 |
| SlowSwingMod | — | MISSING → Phase 15 |
| BowZoomMod | — | MISSING → Phase 15 |
| TargetIndicatorMod | — | MISSING → Phase 15 (or fold into Phase 5's widget) |
| SpawnNPCMod | — | SKIP unless it is purely local and cosmetic; verify against `FairPlayPolicy` first |
| HypixelMod / HypixelQuickPlayMod | — | **SKIP**: server-specific automation. `FairPlayPolicy.validate` runs on every `register`, and the core rule is that anything automating combat/dispatch for a specific server does not belong in Aether |

### Performance / other

| Soar | Aether | Status / action |
| --- | --- | --- |
| FPSBoostMod | `performance.fps_optimizer` | HAVE — Phase 16 expands it (batch rendering, chunk delay, entity/particle filters, font shadow, block effects) |
| FPSLimiterMod | `performance.fps_limiter` | HAVE |
| FarCameraMod | — | MISSING → Phase 16 |
| SoundModifierMod | — | MISSING → Phase 16 (the `AUDIO` category exists for it) |
| BorderlessFullscreenMod | — | MISSING → Phase 16 (display-mode change; document the LWJGL 2 risk) |
| FPSSpooferMod | — | **SKIP-able / flag it**: it exists only to misreport FPS. Either make it display-only with a clear label or do not ship it |
| ForgeSpooferMod | — | **SKIP**: spoofing the client/Forge identity to a server is deceptive and out of scope |
| ClickEffectMod | — | MISSING → Phase 17 (GUI polish) |
| ClickGUIMod / HUDMod | `AetherGui` / the HUD system | n/a — these are Aether's own systems |

**Rules this table encodes**

1. A Soar module that has an Aether equivalent is never re-added; the Aether id, settings and
   architecture stay, and the *behaviour* is ported into it.
2. Every `MISSING` row must ship with working settings and no placeholder UI, or it does not ship.
3. `SKIP` rows are a decision, not an omission: they are recorded here so a later pass does not
   “finish the list” by adding them.

## 14. Which class owns / updates / renders state

| State | Owner | Updated by | Rendered by |
| --- | --- | --- | --- |
| module enabled + settings | the module instance | screens, bridges (`setSetting*` helpers), registry config apply | every consumer |
| HUD geometry | `HudLayout`/`HudElement` | HUD editor screen, config apply | `ForgeHudRenderer` |
| camera (freelook) | `FreelookView` | `ForgeClientEventBridge` (per frame, from `MixinFeatures.Mouse`) | `CameraSetup` event |
| per-frame mouse delta (HUD) | `ForgeClientEventBridge.frameDeltaX/Y` | the same single drain as the camera, in `onCameraSetup` | Mouse Display widget via `frameMouseDelta()` |
| zoom | `MixinFeatures.EntityRenderer` | bridge sets target; mixin animates per frame | `getFOVModifier` mixin |
| hurt shake / hit tint / time / weather | `MixinFeatures` | bridge per tick | the owning mixin |
| animation scalars | the renderer/bridge that draws them | `FrameClock` + `Anim.update()` | the same class |
| theme | `AetherClient.theme()` (cached) | theme modules (the Themes screen just enables one; registry group exclusivity does the rest) | `AetherUi` tokens, renderers |
| open section + client preferences | `ClientPreferences` | the settings screen and `GuiSection` steering, config apply | `AetherGui` chooses the screen, `AetherGuiScreen` lights the matching tile |
| wheel direction | `ClientPreferences.invertScroll()` | the settings screen's `Invert Scroll` toggle | `AetherGuiScreen.scrollDelta`, read by every pager and the HUD editor |

## 16. The Leaf-sourced GUI art (how to change it safely)

The art is generated, never hand-edited:

```
python scripts/leaf_assets.py --analyze    # classification table over Leaf's source textures
python scripts/leaf_assets.py --convert     # write src/main/resources/assets/aether/leaf/**
python scripts/leaf_assets.py --sheet       # build/leaf-sheet.html: source next to result
```

- The source checkout path comes from `LEAF_SOURCE` (default: the Leaf clone under the system temp
  directory). `--ascii NAME` prints one texture as luminance + alpha, which is a shape check only —
  **a black glass body and a transparent pixel both print as blank**, so a dropped icon is invisible
  there. `--sheet` exists for exactly that reason: check the result as an image.
- The conversion is driven by the **alpha plateau** (the interior of the shape), not by luminance:
  Leaf's baked drop shadows share the icons' dark tone, so `surface_mask()` separates them and
  `body_luminance()` finds the panel's own tone per file. Changing a rule means re-running
  `--convert` and re-inspecting the sheet; a stale asset is a black rectangle at runtime.
- Runtime side: `LeafArt` (asset names, `draw`, `drawHovered`) over
  `Mc189Compat.drawTextureTinted`. Draw calls happen in design units, so screens convert with
  `GuiScale` exactly as they do for text and rects.
- Geometry that is a *copy* of Leaf's (nav tiles, card size, gear offsets, scrollbar rect, cosmetics
  rects, the backdrops' panel bounds) is documented in `docs/GUI_REBUILD.md` §2 with the measured
  panel rectangles; a control that falls outside those rectangles is a bug, not a style choice.
