# Aether

Aether is a legitimate Minecraft 1.8.9 PvP client foundation focused on performance, customization, cosmetics, and polished UI. It intentionally excludes cheats, exploits, automation, and gameplay advantages.

This repository contains a Java 8 client core plus a Forge 1.8.9 adapter. The core stays free of Minecraft dependencies; Forge code attaches to it through small adapters for lifecycle, keybinds, HUD rendering, and screens.

## What Exists

- Fair-play policy guardrails for prohibited modules and naming.
- Lightweight synchronous event bus.
- Module metadata, lifecycle, persisted settings, favorites, and registry. Settings carry their own UI metadata (`Setting.Range` bounds and `Setting.choices` option lists), so no screen keeps a slider or choice table that can drift from the module.
- HUD layout model with movement, scaling, opacity, layering, and grid snapping.
- Five theme modules (Aether Blue, Midnight, Aurora, Frost, Light) that resolve to one shared palette hub: `AetherUi` derives every surface, text, accent and edge token from the active palette, and every Aether screen plus the HUD paint from it, so a theme switch repaints the whole client in a frame. Enabling one theme disables the others.
- JSON-backed configuration persistence for simple key/value settings.
- Forge 1.8.9 main menu, mod menu, cosmetics screen, keybinds, HUD renderer, and client effect bridge.
- Aether Click Deck: the primary click GUI (view ribbon, inline accordion settings with sliders driven by each setting's own bounds, colour palettes, choice pills and keybind capture, live telemetry spine, keyboard-first navigation).
- Headless Click Deck self-test that drives layout and input against the 1.8.9 stubs.
- CheatBreaker-inspired fair-play module set for HUD, PvP utilities, performance, render, interface, cosmetics, and themes. 57 modules reach the registry, all consumed by the adapter (see the audit), with in-world capes, wings, halos, hats, trails and custom name tags.
- Per-element HUD controls that are actually read: `mode`, `style`, `format`, `fade_time`, `show_damage`, custom coordinate lines, per-element colours and backgrounds.
- Chat timestamps and a fading notification toast stack fed by client events.
- 1.7 first-person item animations (`graphics.animation`): the blocking sword and food/drink take the live arm swing, the bow draw uses the 1.7 easing curve, and a cast rod sits in the 1.7 position. The pose rules are unit tested in core and applied by `ItemRendererMixin`.
- Mixins wired and registered: `src/forge189/resources/mixins.aether.json` (declared through the jar manifest) enables the item renderer, entity renderer and renderer living entity mixins, which cover the 1.7 poses, the scaled hurt-camera shake for `graphics.no_hurt_cam`, and the damage overlay tint for `graphics.hit_color`. Obfuscated builds generate a refmap with `./gradlew forge189Jar -PaetherSrgMappings=/path/to/mcp-srg.srg`; see the audit for the full setup.
- Smoke tests that compile and run with plain `javac`.

## Verify

```bash
sh scripts/verify.sh
```

The script compiles `src/main/java`, `src/test/java`, and the Forge 1.8.9 adapter, then runs the smoke tests plus the Click Deck self-test. When the Forge dev jar is missing it falls back to compiling the adapter against `src/forge189Stubs`, so the check still covers the UI layer.

Gradle is pinned to Java 8 through the project-local `./gradlew` shim:

```bash
./gradlew build
```

The shim uses `/usr/lib/jvm/java-8-openjdk` and a Java-8-compatible Gradle 7.6.4 distribution from the local Gradle cache. Set `JAVA8_HOME` or `AETHER_GRADLE_HOME` if your paths differ.

The Forge 1.8.9 adapter compiles against the cached Forge dev jar and produces:

```text
build/libs/aether-0.1.0-forge189.jar
```

Set `AETHER_FORGE_189_JAR` if your Forge 1.8.9 dev/deobf jar is somewhere else.

## Next Milestones

1. Add direct-manipulation HUD editing for drag, snap, scale, and opacity.
2. Expand implemented renderers for armor, potion, CPS, ping, crosshair, and cosmetic previews.
3. Add a launcher module for profiles, Java detection, launch logs, repair, and safe mode flows.
4. Add profile presets and stronger import/export flows for module layouts.

See [docs/ROADMAP.md](docs/ROADMAP.md) for the incremental plan, [docs/CLOUDCLIENT_COMPARISON.md](docs/CLOUDCLIENT_COMPARISON.md) for how Aether differs from CloudClient, and [docs/MODULE_AUDIT.md](docs/MODULE_AUDIT.md) for the per-module wiring status (what is wired, what is partial, and how the numbers are reproduced).
