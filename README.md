# Aether

Aether is a legitimate Minecraft 1.8.9 PvP client foundation focused on performance, customization, cosmetics, and polished UI. It intentionally excludes cheats, exploits, automation, and gameplay advantages.

This repository contains a Java 8 client core plus a Forge 1.8.9 adapter. The core stays free of Minecraft dependencies; Forge code attaches to it through small adapters for lifecycle, keybinds, HUD rendering, and screens.

## What Exists

- Fair-play policy guardrails for prohibited modules and naming.
- Lightweight synchronous event bus.
- Module metadata, lifecycle, persisted settings, favorites, and registry.
- HUD layout model with movement, scaling, opacity, layering, and grid snapping.
- Theme palette tokens inspired by Aether's sky/frosted-glass design language.
- JSON-backed configuration persistence for simple key/value settings.
- Forge 1.8.9 main menu, mod menu, cosmetics screen, keybinds, HUD renderer, and client effect bridge.
- CheatBreaker-inspired fair-play module set for HUD, PvP utilities, performance, render, interface, cosmetics, and themes.
- Smoke tests that compile and run with plain `javac`.

## Verify

```bash
sh scripts/verify.sh
```

The script compiles `src/main/java`, `src/test/java`, and the Forge 1.8.9 adapter, then runs the smoke tests.

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

See [docs/ROADMAP.md](docs/ROADMAP.md) for the incremental plan.
