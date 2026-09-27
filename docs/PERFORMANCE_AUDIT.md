# Performance audit

Pass date: 2026-09-27. Companion to `docs/MODULE_LOGIC_AUDIT.md`, which records the
correctness and state work from the same pass.

The single most important statement in this document: **no runtime performance claim is
made here.** Nothing in this repository can start Minecraft 1.8.9, so there is no FPS,
frame-time, allocation or CPU measurement behind any number below. Everything in section 2
is a *static* finding - a count of work per event, per tick or per frame read out of the
code. Section 3 says exactly what was and was not executed. Section 5 lists what must be
profiled before anyone is allowed to say Aether is faster than CloudClient or than an
earlier Aether build.

## 1. Method

* Every Forge event handler in `src/forge189` was read and its frequency recorded
  (`ClientTickEvent`, `MouseEvent`, `MouseInputEvent`, `InputEvent.KeyInputEvent`,
  `DrawBlockHighlightEvent`, `RenderLivingEvent.Post`,
  `RenderLivingEvent.Specials.Pre`, `RenderWorldLastEvent`,
  `ClientChatReceivedEvent`, `EntityViewRenderEvent.CameraSetup`,
  `EntityViewRenderEvent.FogDensity`, `AttackEntityEvent`, and the HUD render events).
* The HUD renderer, the cosmetic renderer, the name-tag renderer, the scoreboard
  replacement renderer and the Click Deck were read for per-frame allocation and repeated
  computation.
* Cache cadences were checked against the rule "only cache what updates slower than it is
  drawn".
* Two per-frame allocations were removed; everything else that could be a hot-path
  problem was either already cached or deliberately left alone (section 4).

## 2. Static findings

### 2.1 Adapter work per tick (20 Hz)

| Handler | Before | After |
| --- | --- | --- |
| `onClientTick` effects | 6 `GameSettings` field reads for zoom, an FOV read + FOV write while zoomed, a world-time read + world-time write, a world rain write, plus a `setKeyBindState` write for each of sprint/sneak every tick | 1 key read, 1 flag publication, 1 cloud-flag comparison, 1 gamma comparison, 3 video-setting comparisons; no world writes, no FOV writes |
| sprint/sneak forcing | 2 `KeyBinding.setKeyBindState` calls per tick | 0 calls while the forced state is unchanged, 1 call on each real transition, 1 on disable |
| fullbright | gamma written every tick | written only when the value differs |
| zoom scroll | `client.save()` (JSON file write) per wheel notch | 1 debounced save 800 ms after the wheel stops |
| time changer | `setWorldTime` per tick | 1 `getWorldTime` read + 3 static field writes |
| weather toggle | 6 reflective invokes per tick (`setRainStrength`, `setThunderStrength`, `setRaining`, `setThundering`, `setRainTime`, `setThunderTime`) plus a `worldInfo` lookup | 0 invokes, 1 boolean publication |

### 2.2 Adapter work per rendered frame

| Handler | Cost | Note |
| --- | --- | --- |
| `onCameraSetup` | 1 boolean check; 3 field writes only while freelook is held | no allocation |
| `EntityRendererMixin.getFOVModifier` | 2 float comparisons and a return while zoom is idle | the zoom animation only runs while a zoom is in flight |
| `WorldMixin.getCelestialAngle` | 1 boolean check when the time changer is off; when on, one `Math.cos` (the same cost vanilla already pays) | no allocation |
| `WorldMixin.getRainStrength` | 1 boolean check when suppressed | no allocation |
| `RenderWorldLastEvent` | delegates to the cosmetic renderer and the name-tag renderer | unchanged |
| `RenderLivingEvent.Post` (hit colour) | 1 boolean + 1 hurt-timer read; when active, one `AxisAlignedBB.offset` pair | unchanged from before, now wrapped in `try/finally`; the two short-lived `AxisAlignedBB` objects per hurt entity are the only allocation this handler makes |
| `DrawBlockHighlightEvent` (block overlay) | guard chain, then 6 quads / 12 lines | unchanged; the `expand`/`offset` pair allocates a small bounding box per frame, as vanilla's own highlight path does |
| `MouseEvent` (freelook) | 1 boolean, then arithmetic on floats - no allocation | only runs while the key is held |
| `onClientTick` effects | see 2.1 | one boxed `Float`/`Boolean`/`Integer` per captured value, once, not per tick |
| HUD render | see 2.3 | |
| Click Deck render | layout pass over the registered modules on the *paused* game clock, rows drawn only inside the viewport | a GUI screen's frame cost does not run against world rendering |

### 2.3 HUD cache cadence (the intended budget)

| Cadence | What is cached | Where |
| --- | --- | --- |
| Per frame | actual drawing, positions, colours, the enabled-flag lookups the visible elements need | `ForgeHudRenderer` |
| On change only | FPS text (`cachedFpsText` is rewritten only when the FPS integer changes), block-info name (**new**: keyed on the block's unlocalised name + metadata) | `renderFps`, `renderBlockInfo` |
| 250 ms | FPS history sample | `sampleFps` |
| 500 ms | memory text, developer overlay text | `renderMemory`, `renderDeveloperOverlay` |
| 1 s | clock text (also invalidated when the 12h/24h setting changes) | `renderClock` |
| Event driven | combo counter, notifications, module/config changes, click-deck telemetry snapshot | `registerComboHit`, `notifications`, `AetherClient` |

Everything else that is drawn every frame (coordinates, reach, speed, direction, CPS,
keystrokes, ping, day counter, scoreboard lines) genuinely changes per frame or per tick,
so caching it would add staleness for no measurable gain. The scoreboard replacement
renderer keeps reused scratch lists (`reusableFilteredScores`, `reusableFormattedLines`,
`reusableFormattedScores`) so a re-render does not allocate new lists, and the HUD's
`ScaledResolution` objects are per frame as they are in vanilla.

### 2.4 Removed per-frame work

* Block Info no longer builds an `ItemStack` and calls `getDisplayName()` (two strings and
  an item stack per frame) every frame; it caches the name against the block and its
  metadata and reuses the string in place.
* Chat timestamps no longer build a `SimpleDateFormat` (and parse its pattern) for every
  incoming line; the two formatters are built once and reused. This is an event-rate win
  rather than a per-frame one, but it was the last allocation of that shape outside the HUD.
* The HUD editor's snapping search re-ran on every frame of a drag even when the cursor had
  not moved; it now re-runs only when the cursor actually moves and otherwise keeps the
  guide lines it already computed. The search itself was already interaction-only (it runs
  while an element is being dragged, never when idle) and its cost is O(elements) with the
  element count in the low tens, so this is the only change it needed - no early-reject
  structure was added for a list that size.

### 2.5 Texture and GL state

* The cosmetic renderer caches one `DynamicTexture` per cape id in `capeTextures`, remembers
  unusable capes in `unreadableCapes`, and never re-uploads an image that has already been
  read. No `ResourceLocation` or texture is created in the render pass, and the cape image
  is read from disk at most once per cape id per session.
* Custom rendering that touches global GL state (block overlay, hit outline, name tags,
  scoreboard, HUD) restores that state in `finally` blocks. The hit outline gained a
  colour/line-width/blend restore it previously lacked, which also removes a class of
  "everything after this draw is tinted" bug rather than a measurable cost.
* CloudClient's habit of binding textures and creating `ResourceLocation`s inside image
  helpers is deliberately **not** reproduced anywhere in this pass.

### 2.6 What was deliberately *not* optimised

* **Click Deck layout.** `computeLayout` runs once per frame and once per click, which is
  what keeps painting and hit-testing from drifting apart (the previous screen recomputed
  the same maths inside its click handler). The screen pauses the world; the pass is O(60)
  boxes with small allocations, and rows outside the viewport are already skipped when
  drawing. Caching it would introduce an invalidation problem for a cost nobody has
  measured.
* **Per-frame coordinate/reach/speed formatting.** These values change every frame; the
  strings are small and short-lived, which is precisely the allocation profile the young
  generation handles best. Changing them would trade correctness risk for an unmeasurable
  win.
* **Event handler registration.** Handlers stay always-registered and are gated by module
  state. Dynamic registration would move work from "one boolean check per event" to
  "registration bookkeeping on every module toggle" and make the failure modes harder to
  reason about, for no measured benefit.
* **FPS Optimizer's `System.gc()` path.** Kept, but it is behind a setting and a 45-second
  timer with a 70% heap-fill precondition. It is the one place in the client that asks for
  a full collection; if anyone profiles a hitch after enabling the optimizer, this is the
  first line to look at.

## 3. Measurements taken

| What was run | Result |
| --- | --- |
| Core build (`javac --release 8`, all of `src/main/java`) | compiles clean |
| Stub build (`src/forge189Stubs`) | compiles clean |
| Adapter build (`src/forge189` against the stubs) | compiles clean |
| Test build + 16 test classes (9 pre-existing, 6 new core classes, 1 adapter self-test) | all pass |
| `AetherClickDeckSelfTest` (headless deck drive: draw, keyboard, mouse, palette, reset) | 17 checks pass |
| Registry metadata dump (`build/audit/ModuleDump`) | 57 modules, 183 settings, 47 ranges, 19 choice lists |

**Not measured, and therefore not claimed:** FPS, frame time, per-frame allocation rate,
GC pause distribution, CPU time per handler, Click Deck open/close cost, HUD-heavy frame
cost, cosmetics-heavy server cost, zoom/freelook overhead. No in-game profiling is possible
in this environment (no Forge runtime, no Minecraft jar).

## 4. Known limitations of this audit

1. Static counts are read from the code, not observed. A reflective `Mc189Compat` lookup is
   cached per class/name pair, but nothing here proves how often a cache miss happens on a
   real client (it should only ever happen once per field or method).
2. The mixin hooks' real cost depends on how often vanilla calls the intercepted methods.
   `getFOVModifier` is called a handful of times per frame (camera setup plus the mouse-over
   ray); `getCelestialAngle` is called several times per frame for sky, fog, light and star
   values. Both hooks return on a boolean or two when their module is off.
3. Different render distances, resource packs, other mods (OptiFine in particular
   reimplements the FOV and lightmap paths) will change the frame profile far more than
   anything in this pass.
4. The Click Deck's cost was reasoned about, not measured; it is a paused-world screen, so
   its cost should be compared against the world frame it replaces, not added to it.
5. `System.gc()` in FPS Optimizer is a deliberate feature toggle, not an optimization. It is
   listed here because it is the only action in the client that can cause a multi-frame
   hitch on purpose.

## 5. Areas needing profiling (and what to look for)

Suggested method: a Java Flight Recorder profile (`-XX:StartFlightRecording`) or a
sampling profiler over a fixed scenario, comparing an Aether build with all modules off
against the same build with one module at a time on. The following are the findings that
are most worth a measurement, in the order they are likely to matter:

1. **Click Deck frame cost** (`AetherClickGuiScreen.render` -> `computeLayout`, `drawRows`,
   `drawSpine`). Look for allocation in `new Box(...)` per module per frame and for the
   telemetry/sparkline work. If a deck frame shows up hot, the fix is to keep the layout
   pass but cache the *telemetry* snapshot, not to cache the layout.
2. **Cosmetic render pass** (`ForgeCosmeticRenderer.onRenderWorldLast`) on a full server.
   The per-player loop is capped by `cosmetics.player_preview`'s `max_distance` and
   `skip_invisible`, so measure with those at their extremes (2 blocks vs 32) and confirm
   the cull is what dominates. Watch for geometry rebuilds per player per frame.
3. **Name tag pass** (`ForgeNameTagRenderer.onRenderWorldLast`) with a crowded server, and
   specifically the string/width work per tag per frame.
4. **HUD with everything enabled.** Confirm the cache cadences in 2.3 hold (FPS text should
   be rewritten a few times per second, memory twice a second, clock once) and look for
   `ScaledResolution` churn.
5. **Scoreboard replacement renderer** on a server with a long sidebar: it measures every
   line's width each frame. If it profiles hot, cache the measured widths against the
   objective's contents instead of against the frame.
6. **Zoom and freelook active.** Both should be flat: zoom does one multiply per FOV query
   while animating and nothing while idle, freelook publishes three fields per tick and
   draws nothing on its own.
7. **FPS Optimizer** for hitches (see 2.6 - `System.gc()`).
8. **Weather and time modules on a server that changes weather/time**, to confirm no
   per-frame allocation appears in the two `WorldMixin` hooks.

Measurements should be reported with the scenario, render distance, module set and the
profiler used, and compared against the same scenario on the previous build - not against
CloudClient, which cannot be run here either and whose modules are architecturally
different.

---

# Addendum: Aether 2.0 pass (same day)

The no-runtime-measurement rule still applies: everything below is static.

## 6. Control Center

| Cost | Before (Click Deck) | After (Control Center) |
| --- | --- | --- |
| Module search | every render frame re-scored every visible module against the query (`rebuildRows` in the paint path) | `ModuleSearch` recomputes only when query/category/filters/source change; `results()` returns one reused list instance |
| Section pages | did not exist | Profiles/Screenshots/Settings are plain data reads; the screenshot listing is throttled to one directory read per 2 s |
| Sidebar hit-testing | n/a | registered during layout alongside module boxes; zero extra passes |
| Row rendering | unchanged | unchanged: viewport culling kept, hover blending kept |
| Text measurement | unchanged | label widths for waypoint markers are cached per rounded distance (new); deck strings unchanged |

## 7. Screenshots

- The render thread pays one `glReadPixels` + one array copy per capture, only when
  the player asks for one. No per-frame cost while idle (a boolean check).
- PNG encode + disk write run on a single daemon worker; a slow encode cannot stall
  a frame, and captures queue behind each other instead of racing the disk.
- The Screenshots page lists metadata only (name/size/mtime); it never decodes
  pixels to draw the list. Thumbnails are deliberately not implemented until there
  is a cache with a decode budget to back them.

## 8. Waypoints

- Per frame: one dimension read, one list filter, and for each enabled waypoint of
  the current dimension a distance computation. Beyond 512 blocks the cost is the
  distance computation only.
- The distance label is re-measured only when its rounded distance changes; the
  cache is a fixed 8-slot array, no allocation per frame.
- The draw path reuses the block-overlay line drawing (no new tessellator pipeline)
  and restores blend/texture/depth/line-width/matrix in a `finally`.

## 9. FPS limiter

`performance.fps_limiter` drives vanilla's own framerate cap through `ValueHold`
(captured once, restored once, writes only on change) - it adds no loop of its own
and no polling beyond the existing tick handler.

## 10. What must be measured in game

Unchanged from section 5, plus: the Control Center's frame cost at 60 vs 59-module
lists, the screenshot capture hitch (should be one glReadPixels), and waypoint
rendering with 100+ enabled waypoints at close range.
