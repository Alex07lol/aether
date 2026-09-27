# Roadmap

## Phase 1: Core Foundation

- Fair-play guardrails. Done.
- Event bus. Done.
- Module registry and persisted settings. Done.
- HUD layout model. Done.
- Theme tokens. Done.
- JSON config persistence. Done.
- Compileable smoke tests. Done.

## Phase 2: Minecraft 1.8.9 Adapter

- Forge lifecycle integration. Done.
- Keybind mapping. Done.
- HUD render pass. Done.
- Main menu, mod menu, and cosmetics screens. Done.
- Client effect bridge for fair-play visual/performance modules. Done.
- Chat event bridge for statistics. Done (timestamps, plus the notify toast stack).
- Screenshot and resource-pack hooks. Open: the orphan screenshot module was deleted rather than left inert.
- Safe defaults for server-specific profiles.

## Phase 3: In-Game UI

- Frosted-glass mod menu. Done.
- Original Click Deck click GUI (view ribbon, inline accordion settings, colour palette, keybind capture, telemetry spine, keyboard navigation). Done.
- Headless Click Deck self-test wired into `scripts/verify.sh`. Done.
- Direct-manipulation HUD editor.
- Settings search, import/export, favorites, and tooltips. Done (search and favourites in the deck; import/export still open).
- Interactive setting editing and reset actions. Done (sliders, switches, palettes, keybind capture, text fields, per-setting and per-module reset).
- Min/max and choice metadata on core `Setting`, so no screen needs a range table. Done: `Setting.Range` / `Setting.choices`, ranged `addNumber` and option-carrying `addChoice` overloads, 47 ranged and 19 choice settings, guarded by `AetherSettingsMetadataTest`.
- Theme manager with accessible contrast checks.

## Phase 7: Adapter Wiring Backlog

Tracked in [MODULE_AUDIT.md](MODULE_AUDIT.md). All 57 registered modules are wired
and every setting is read; what is left is depth rather than coverage:

- Finish `graphics.animation`. Done: BLOCK and EAT/DRINK take the live arm swing and
  the bow draw follows the 1.7 curve through `ItemRendererMixin`, with the pose rules
  unit tested in `FirstPersonAnimsTest`.
- Register the entity renderer and renderer-living-entity mixins in
  `mixins.aether.json`. Done: all three mixins load, and the two new ones drive
  `graphics.no_hurt_cam` (scaled camera shake) and `graphics.hit_color` (damage
  overlay tint) from bridge-published flags.
- Generate the refmap for obfuscated builds. Done as an opt-in build step
  (`-PaetherSrgMappings=...` plus the Forge dev jar) that refuses to run without
  both; still to do is exercising it on a machine that has them.
- Give `graphics.custom_crosshair` a per-element layout editor instead of one shape.
- Add easing helpers for panel transitions in the deck and other screens.
- Done in the wiring pass: `Setting` metadata (bounds + options) with the deck and
  the legacy manager reading it, the cosmetics tree registered plus an in-world
  renderer for capes/wings/halos/hats/trails, custom name tags with vanilla-tag
  suppression, chat timestamps, the notification toast stack, and the deletion of
  the six modules that had no possible hook (`graphics.item_physics`,
  `graphics.motion_blur`, `interface.crosshair_editor`, `interface.gui_tweaks`,
  `interface.screenshot_manager`, `interface.scroll_tooltips`).

## Phase 4: Launcher

- Profile manager.
- Java detection and memory allocation.
- Launch logs, repair mode, safe mode, and portable mode.
- News/changelog panels.

## Phase 5: Cosmetics

- PNG cape upload validation. Done (`CosmeticLibrary`).
- In-world renderer and slot selection (cape, wings, halo, hat, trail). Done.
- Animated cape metadata.
- Local cache and sync-ready IDs.
- Outfit presets and import/export packs.

## Phase 6: Performance

- Entity culling adapter.
- Particle limiter adapter.
- Smart animation toggles.
- Allocation and frame-time instrumentation.
- Developer overlay.
