# Module Audit (Forge 1.8.9 adapter)

Re-measured on `master` after the wiring pass that added `Setting` metadata, the
cosmetics renderer and the name tag / chat / notification hooks. Every module
class under `src/main/java/dev/aether/module/impl` was checked for three things:

1. **Registered** - is the module added to the registry by `BuiltInModules`?
2. **Wired** - does any code in `src/forge189` reference the module id, so that
   enabling it changes something in game?
3. **Settings honoured** - is each declared setting consumed by the adapter, or
   is it stored and then ignored?

> Follow-up passes on the same 57 modules: `docs/MODULE_LOGIC_AUDIT.md` (module
> state, lifecycle, mixin scope, side effects and the CloudClient classification)
> and `docs/PERFORMANCE_AUDIT.md` (static hot-path findings, cache cadences and the
> profiling work that is still outstanding).

## Summary

| Status | Count | Meaning |
| --- | --- | --- |
| Registered module classes | 57 | Every class in `impl` reaches the registry; no orphans left |
| Wired in the adapter | 57 | Adapter code reads the id, so a switch changes something |
| Settings honoured | 183 of 183 | No setting is stored and then ignored |
| Not registered | 0 | The four orphan classes were deleted |
| `STUB` rows in the deck | 0 | The deck no longer needs a stub badge |

Categories: HUD 17, PvP 6, Performance 2, Graphics 11, Interface 7, Themes 5,
Cosmetics 9.

Fair-play review: no module in the tree trips `FairPlayPolicy`. The closest calls
are informational/perspective helpers (`hud.reach_display` only reports the
distance of the last hit, `pvp.freelook`/`pvp.snaplook` only move the camera, and
`pvp.toggle_sneak` drives the vanilla key bind), and `docs/FAIR_PLAY.md` already
flags that some servers still restrict those.

## Reproduce the numbers

```bash
# registered module classes
grep -c 'modules.register' src/main/java/dev/aether/module/builtin/*Modules.java

# settings declared but never read with their module id next to them
for f in $(grep -rl 'ModuleMetadata.builder' src/main/java/dev/aether/module/impl); do
  cls=$(basename "$f" .java)
  if grep -q 'builder(ID' "$f"; then
    mid=$(grep -o 'String ID = "[^"]*"' "$f" | head -1 | sed 's/.*"\(.*\)"/\1/'); key="$cls.ID"
  else
    mid=$(grep -o 'builder("[^"]*"' "$f" | head -1 | sed 's/builder("\(.*\)"/\1/'); key="\"$mid\""
  fi
  for s in $(grep -o 'add[A-Za-z]*("[a-z_0-9]*"' "$f" | sed 's/add[A-Za-z]*("\(.*\)"/\1/'); do
    grep -Rn --include=*.java -e "$key[^;]*\"$s\"" src/forge189/java >/dev/null || echo "IGNORED $mid -> $s"
  done
done
```

Six settings are read through a helper that hard-codes the module id instead of
repeating it on the line: `interface.notifications`'s three settings (read by
`ForgeNotifications`) and `pvp.freelook`'s three (read through
`freelookModuleId()`), so the loop above lists those six as `IGNORED` false
positives. Spot-check them with `grep -rn 'NotificationsModule.ID' src/forge189`
and `grep -n 'freelookModuleId' src/forge189/java/dev/aether/forge189/ForgeClientEventBridge.java`.

The same guarantees are enforced by tests rather than by eyeballing:
`AetherSettingsMetadataTest` walks the registry and fails on a setting without
metadata, and `AetherClickDeckSelfTest` drives the deck against the 1.8.9 stubs.
`scripts/verify.sh` runs both.

## What this pass changed

* **`Setting` carries its own metadata.** `Setting.Range(min, max, step)` and
  `Setting.choices(...)` live on the setting, `AbstractModule` gained ranged
  `addNumber(...)` and option-carrying `addChoice(...)` overloads, and both GUIs
  read that metadata instead of keeping id-keyed tables. 47 of the 183 settings
  now declare slider bounds and 19 declare option lists, and unknown values
  normalise on load (clamped numbers, first choice fallback) so a stale config
  cannot leave a module in an unrepresentable state.
* **Cosmetics are real.** `CosmeticModules.register()` is called from
  `BuiltInModules.registerAll`, so all nine cosmetic modules load, and
  `ForgeCosmeticRenderer` draws capes, wings, halos, hats and trails in the
  world-last pass, reading each slot's own settings. `AetherCosmeticsScreen`
  drives the same library.
* **Name tags are real.** `ForgeNameTagRenderer` cancels the vanilla tag
  (`RenderLivingEvent.Specials.Pre`) and draws its own billboarded tag with the
  module's scale, colours, background and armour value; the local player's tag
  uses `interface.nick_hider`'s nickname.
* **Chat and notifications are real.** `interface.chat_customization` rewrites
  incoming chat with a coloured timestamp (ARGB mapped to the nearest legacy
  colour) and `interface.notifications` renders a fading toast stack fed by
  toggle-sprint/sneak flips and combo milestones.
* **Six modules were deleted, not faked.** `graphics.item_physics`,
  `graphics.motion_blur`, `interface.crosshair_editor`, `interface.gui_tweaks`,
  `interface.screenshot_manager` and `interface.scroll_tooltips` had no possible
  hook in this adapter, so keeping their switches would only advertise features
  that cannot work. `graphics.custom_crosshair` already covers the crosshair
  case.
* **The animation module's first-person poses are in.** Previously only the arm-swing loop
  and the third-person sword pose were wired; the per-action first-person poses now
  come from one mixin plus unit-tested core math (see the graphics section).
* **The ignored-settings sweep is complete.** Every declared `mode`, `style`,
  `format`, `fade_time` and `show_damage` is read: the HUD `mode` pills,
  `hud.direction.style`, `hud.clock.format`, `hud.keystrokes.fade_time` (keys
  fade out on release), `hud.armor.show_damage` and `hud.fps_graph.graph_mode`.
* **The deck is honest by construction.** With no unwired modules left, the
  `STUB` tag and the telemetry spine's "not wired yet" counter are gone; the
  deck has no status list that could drift from reality.

## Status by category

### HUD (17 registered) - all settings read

`hud.fps`, `hud.coordinates` (`mode`, `hide_y`, `custom_line`, `show_direction`,
`direction_color`), `hud.keystrokes` (`fade_time`, box sizing, arrows, clicks,
spacebar), `hud.cps`, `hud.potions` (`mode`, `hide_ambient`, background,
colours), `hud.clock` (`format`), `hud.combo` (live hit counter from
`AttackEntityEvent`), `hud.memory`, `hud.ping`, `hud.reach_display`,
`hud.speed_indicator`, `hud.server_address`, `hud.direction` (`style`),
`hud.block_info`, `hud.armor` (`show_durability` + `show_damage` combine into
count/percent), `hud.fps_graph` (`graph_mode` sparkline vs bars), `hud.day_counter`.

### PvP (6 registered) - all settings read

`pvp.block_overlay` (outline/fill/thickness/colours), `pvp.freelook` (keybind,
sensitivity, invert; perspective restored on disable), `pvp.snaplook`,
`pvp.toggle_sprint` (keybind, `mode`, HUD status line), `pvp.toggle_sneak`
(keybind drives the sneak bind, `mode`/`show_status` drive the HUD line),
`pvp.zoom` (keybind, percent, min/max, scroll step).

### Performance (2 registered)

`performance.fps_optimizer` (fast graphics, VBO, fast lighting, memory cleanup -
each restored on disable) and `developer.overlay` (F12 toggle, HUD line, no
settings by design).

### Graphics (11 registered)

`graphics.custom_crosshair`, `graphics.fullbright`, `graphics.hit_color`,
`graphics.sky_customization`, `graphics.time_changer`, `graphics.ui_blur`,
`graphics.particles`, `graphics.weather_toggle`, `graphics.nametag` (custom tag
pass), `graphics.no_hurt_cam` and `graphics.animation` are wired.

`graphics.no_hurt_cam`'s `shake_amount` is applied where the shake happens: the
entity renderer mixin replaces `hurtCameraEffect` with the same rotations at the
configured scale, so the player's hurt timers are never rewritten by the client.
`graphics.hit_color` works in two places on purpose - the renderer living entity
mixin tints the damage overlay, and the bridge draws the hit outline around the
entity in the same colour.

`graphics.animation` covers all four toggles:

* `block_animation` drives the third-person sword pose, the inline block
  transformation, and the 1.7 first-person block swing.
* `eat_drink_animation` gives food and potions the 1.7 first-person swing.
* `bow_animation` puts vanilla's bow pose on the 1.7 draw curve (a square rather
  than 1.8's smoothstep).
* `rod_animation` holds a cast rod in the 1.7 position.

The first-person poses run through `ItemRendererMixin`, which redirects the item
transform and bow calls in `renderItemInFirstPerson` instead of reimplementing
them: it hands vanilla's own transform the live arm swing for BLOCK and EAT/DRINK
(1.8 passes a frozen `0`, which is the missing bob) and, for the bow, a
tick value that makes vanilla's easing follow the 1.7 curve. The rules live in
`dev.aether.graphics.FirstPersonAnims` and are unit tested without a Minecraft
runtime (`FirstPersonAnimsTest`).  `mixins.aether.json` (declared through the jar
  manifest) is what registers the mixin.

### Mixin runtime

`src/forge189/resources/mixins.aether.json`, declared through the `MixinConfigs`
manifest attribute on the `forge189Jar` task, enables all three mixins: the item
renderer (1.7 poses), the entity renderer (camera shake) and the renderer living
entity (damage overlay tint). Each one reads plain static flags that
`ForgeClientEventBridge.applyClientEffects()` republishes every client tick, so a
module switch is picked up on the next frame and no mixin needs to know about the
module registry.

The four redirects in the renderer living entity mixin are marked
`require = 0`: they depend on the order of the brightness-buffer writes in the
mapped method, and on a runtime where that order differs the tint is skipped and
logged instead of failing the whole mixin. The item renderer and entity renderer
injections keep the default requirement, so a target that disappears is a loud
failure rather than a silent no-op.

Obfuscated builds need a refmap, because the mixin sources name members the way
MCP does and the runtime only has SRG/notch names. The build wires that up without
turning it on by default:

```bash
# once, on a machine that has the ForgeGradle mapping file
./gradlew forge189Jar -PaetherSrgMappings=/path/to/mcp-srg.srg
```

`mixinMappingJar` packages that file as `searge.srg` on the annotation processor's
classpath (the resource name Mixin's searge environment reads), `compileForge189Java`
gets `-AoutRefMapFile` and `-AdefaultObfuscationEnv=searge`, and `forge189Jar` packs
the generated `mixins.aether.refmap.json` next to the classes the config points at
with `"refmap": "mixins.aether.refmap.json"`. The step also requires the Forge dev
jar (`AETHER_FORGE_189_JAR`), because the processor validates every injection target
against the real Minecraft classes; against the local stubs it would correctly
complain. Without the flag the adapter still compiles and the mixins still work in a
development runtime, where the MCP names match.

### Interface (7 registered)

`interface.bossbar`, `interface.scoreboard_customization`,
`interface.hud_editor` and `interface.theme_selector` (both one-shot launchers
that switch themselves back off), `interface.chat_customization` (timestamp
rewrite), `interface.nick_hider` (name tags and the local player's own tag) and
`interface.notifications` (toast stack).

### Themes (5 registered)

`theme.aether_blue`, `theme.midnight`, `theme.aurora`, `theme.frost` and
`theme.light` implement `ThemeModule`, share the `theme` registry group (so only
one can be on) and expose an immutable palette. `AetherClient.theme()` resolves
the enabled module's palette and caches it by identity, so a renderer notices a
switch in one comparison.

The adapter side is `AetherUi`, the single source of truth: `AetherUi.bind(client)`
is called once at mod init, `AetherUi.syncTheme()` repaints every token from the
active palette, and each screen calls it at the top of its render pass - main
menu, quick nav, three-panel module manager, Click Deck, HUD editor, cosmetics,
account manager and info panel. The HUD renderer refreshes its colour fallbacks
the same way. No screen keeps a palette of its own. Light palettes flip the edge
tokens to dark ink so borders stay visible on a bright surface, and
`theme.aether_blue` is the default.

### Cosmetics (9 registered)

`cosmetics.manager` (one-shot launcher for the cosmetics screen),
`cosmetics.current_cape`, `cosmetics.current_wings`, `cosmetics.current_halo`,
`cosmetics.current_hat`, `cosmetics.current_trail`, `cosmetics.trails`,
`cosmetics.player_preview` and `cosmetics.cape_preview` all load, and the slot
modules carry the settings the in-world renderer reads (length, wave, flap
speed, glow, bob, tilt, particle type, rate, spread and so on).
`CosmeticLibrary` handles PNG import + validation + per-slot selection.

## What actually works end to end today

All 57 registered modules reach adapter behaviour and every one of the 183
settings is read somewhere in `src/forge189`, which is why the deck no longer
needs a stub badge. The known gaps are depth, not honesty:
`graphics.custom_crosshair` has no per-element layout editor, cosmetics render
built-in assets only (nothing is downloaded), and the refmap step above has not
been exercised on a machine that has both the Forge dev jar and the MCP mapping
file, so the production obfuscation path is written but unverified.
