# Architecture

Aether should grow as a set of small, testable layers.

## Core

The core layer has no Minecraft dependency. It owns:

- Module metadata and lifecycle.
- Event dispatch.
- Configuration persistence.
- Fair-play validation.
- HUD layout state.
- Theme tokens.

This layer must remain compileable with Java 8 and no external libraries until the project intentionally introduces a dependency manager.

## Adapters

Adapters translate platform events into core events.

- `forge-1.8.9`: Minecraft lifecycle, keybind, render, chat, screenshot, resource pack, and profile hooks.
- `forge-1.7.10`: Compatibility adapter for features that can be supported safely.
- `launcher`: Accounts, Java detection, profiles, launch logs, repair, updates, and safe mode.
- `cloud`: Sync, cosmetics, crash reporting, and sharing IDs.

Adapters should not own product rules. For example, a Forge keybind should call a module lifecycle method; it should not decide whether a module is allowed.

## Fair Play Boundary

Anything that automates combat, changes reach, changes velocity, reveals hidden information, bypasses server rules, or manipulates packets for an advantage belongs outside Aether. The core registry enforces a first line of defense by rejecting prohibited module identifiers and names.

## Data Flow

1. Platform adapter receives input/render/game events.
2. Adapter publishes core events.
3. Modules update local state or render UI-only output.
4. Configuration changes are saved through the config store.
5. UI reads module metadata/settings from the registry.

