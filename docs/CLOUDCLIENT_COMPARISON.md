# Aether vs CloudClient (1.8.9)

Reference revision: `cloudclientdev/cloudclient@4e2752f` (the 1.8.9 tree lives in
`1.8.9/cloudclient`). CloudClient's README marks the project as discontinued and
archived, and it is licensed LGPL-3.0, so this document is a behavioural and
architectural comparison only - no CloudClient source is copied into Aether.

## Shape of the two projects

| | CloudClient 1.8.9 | Aether |
| --- | --- | --- |
| Status | Archived / discontinued | Active (foundation) |
| Build | ForgeGradle 2.1 + Mixin 0.7.11, one Gradle project per MC version | Plain `javac`-friendly Gradle build, Java 8, no mandatory dependencies |
| Core vs platform | One tree; modules call Minecraft directly | MC-free core (`src/main/java`) plus a Forge 1.8.9 adapter (`src/forge189`) and compile stubs (`src/forge189Stubs`) |
| Registry | `ModManager` with 34 mods, `Type` enum (All/Hud/Mechanic/Visual/Tweaks) | `ModuleRegistry` with 57 registered modules, fair-play validated at registration |
| Settings | `Setting` objects with sliders/checkboxes/colour pickers inside the mod menu | `Setting<T>` on each module (bool/number/text/choice/colour/keybind) that also carries its own slider bounds and choice list, JSON persisted |
| Config | JSON via `Config`/`ConfigLoader`/`ConfigSaver`/`ModConfig` | JSON via `JsonConfigStore` + `ConfigDocument` (module states + every setting) |
| HUD editing | `HudEditor` with drag + `SnapPosition` | `HudLayout` (x/y/scale/opacity/layer) + `AetherHudEditorScreen` |
| Click GUI | `ModMenu`: category sidebar, card grid, settings panel; `Style` light/dark | Legacy `AetherModMenuScreen` (same 3-panel idea) replaced by the original **Click Deck** (`AetherClickGuiScreen`) |
| Extras | Panorama title screen, custom font helper, animation helpers, CPS/scroll/position helpers, 13 mixins | Main menu, mod menu, cosmetics screen, account manager, info screen, quick nav, custom glyph font renderer, 3 mixins registered through `mixins.aether.json` (item renderer for the 1.7 poses, entity renderer for the hurt camera, renderer living entity for the damage overlay tint) |
| Tests / CI | none in-tree | `scripts/verify.sh` smoke tests + Forge 1.8.9 adapter compile + GitHub Actions release build |

## Module coverage: CloudClient feature -> Aether equivalent

| CloudClient mod | Aether | Notes |
| --- | --- | --- |
| ToggleSprint | `pvp.toggle_sprint` | Aether also renders an on-HUD status line |
| Fps | `hud.fps` | |
| Keystrokes | `hud.keystrokes` | Aether adds spacebar/arrow/gap/click-size options |
| Armor | `hud.armor` | |
| Fullbright | `graphics.fullbright` | Gamma restore on disable |
| Snaplook | `pvp.snaplook` | |
| Coordinates | `hud.coordinates` | CloudClient has no vertical mode |
| ServerAddress | `hud.server_address` | |
| Ping | `hud.ping` | |
| Cps | `hud.cps` | |
| Potion | `hud.potions` | |
| Time | `hud.clock` | |
| SpeedIndicator | `hud.speed_indicator` | |
| Animation (1.7) | `graphics.animation` | Four toggles are wired: inline block and rod poses, plus first-person poses through `ItemRendererMixin` (live arm swing for BLOCK and EAT/DRINK, the 1.7 bow draw curve) |
| Freelook | `pvp.freelook` | |
| Crosshair (+ layout manager) | `graphics.custom_crosshair` | CloudClient's per-element crosshair layout is richer |
| Motionblur | - | Deleted: this adapter has no frame-blend hook to back it, so the switch was removed rather than faked |
| GuiTweaks | - | Deleted for the same reason: no screen/widget hook exists in the adapter |
| BlockOverlay | `pvp.block_overlay` | |
| BlockInfo | `hud.block_info` | |
| ReachDisplay | `hud.reach_display` | |
| Zoom | `pvp.zoom` | |
| NoHurtCam | `graphics.no_hurt_cam` | |
| ParticleMultiplier | `graphics.particles` | |
| Scoreboard | `interface.scoreboard_customization` | |
| Bossbar | `interface.bossbar` | |
| Direction | `hud.direction` | |
| HitColor | `graphics.hit_color` | |
| TimeChanger | `graphics.time_changer` | |
| NameTag | `graphics.nametag` | Wired: cancels the vanilla tag pass and draws its own billboarded tag with the module's scale, colours, background and optional armour value |
| ScrollTooltips | - | Deleted: no tooltip hook to back it |
| NickHider | `interface.nick_hider` | Wired: renames the local player's own name tag (chat and tab list keep the real name) |
| ToggleSneak | `pvp.toggle_sneak` | Wired: keybind toggles the sneak bind, optional HUD status line |
| DayCounter | `hud.day_counter` | Wired: day number from world time |
| Combo | `hud.combo` | Wired: live hit counter with a configurable reset window |
| UiBlur | `graphics.ui_blur` | Wired: vanilla blur post-shader plus a configurable dim |
| SkyCustomization | `graphics.sky_customization` | Wired: cloud style and a no-fog toggle |

Things Aether has that CloudClient 1.8.9 does not:

- Fair-play policy guardrails that reject prohibited identifiers at registration.
- Cosmetics library (PNG cape import, validation, per-slot selection, 13 cosmetic
  types), a cosmetics screen and an in-world render pass for capes, wings, halos,
  hats and trails.
- Chat timestamps and a toast notification stack, neither of which CloudClient has.
- Theme modules (five palettes) feeding one shared token hub (`AetherUi`) that every screen and the HUD paint from - a single light/dark source of truth, not per-screen constants.
- Combo counter, memory usage, FPS sparkline graph, developer overlay, FPS
  optimizer, chat customization, notifications.
- Account manager screen, info screen, quick nav ring, platform detection.

Things CloudClient does that Aether should copy as *ideas* (not code):

- Per-element crosshair layout editor (`LayoutManager`) instead of one shape.
- ~~Toggle-sneak and day counter, which Aether simply lacks.~~ Added.
- ~~Light/dark "Style" switch that every widget reads from one place~~ Done: the
  theme modules feed `AetherUi`, which every widget reads from.
- Small animation helpers (easing + a delta-time clamp) for panel transitions.

## Click GUI: what changed and why

The old Aether manager copied CloudClient's shape: a sidebar of categories, a
grid of icon cards, and a separate properties column. The Click Deck replaces it
with a different interaction model:

- **View ribbon, not sidebar.** Views are chips across the top
  (`All`, `Live`, `Favorites`, then one chip per populated category); Tab or
  Shift+Tab cycles them.
- **Rows, not cards.** Every module is a single full-width row with a status
  lamp, favourite star, category tag, inline enable switch and a settings
  chevron.
- **Accordion, not properties panel.** Settings expand under the selected row
  and are edited in place: real sliders with drag, a colour palette popover with
  alpha steps, choice pills, keybind capture ("press a key"), and text fields.
- **Keyboard first.** Type to search (fuzzy match on name, id, description,
  category), arrows/Home/End/PageUp/PageDown to move, Enter toggles, Space
  expands, E expands all, C collapses all, R resets the selected module, right
  click resets a row, Escape clears the search then closes.
- **Telemetry spine.** Live FPS sparkline, enabled ratio, per-category bars,
  build/Java/theme/server/config rows, and jump buttons to the HUD editor,
  cosmetics, the legacy list view and a manual save.
- **Honest status.** Rows used to carry a `STUB` tag for modules the adapter did
  not consume yet; the last of those modules were either wired or deleted, so the
  badge and its counter are gone.
- **One layout pass.** Row geometry is computed once per frame and shared by
  painting and hit-testing, so clicks cannot drift from what is drawn (the old
  screen rebuilt the same maths inside its click handler).
- **Headless testable.** `AetherClickDeckSelfTest` drives the deck against the
  1.8.9 stubs (draw, keyboard, mouse, palette, reset) so layout regressions fail
  in CI instead of in game.

## Follow-ups this comparison suggests

1. Mirror CloudClient's crosshair layout manager for `graphics.custom_crosshair`.
2. ~~Add the two features Aether lacks entirely: toggle sneak and a day counter.~~
   Done (`pvp.toggle_sneak`, `hud.day_counter`).
3. ~~Make one theme source of truth that both the theme modules and every screen~~
   Done: `AetherClient.theme()` resolves the enabled `ThemeModule`, `AetherUi`
   derives one token set from that palette, and every Aether screen (plus the HUD
   colour fallbacks) calls `AetherUi.syncTheme()` and draws only from those
   tokens. The deck keeps no palette of its own.
4. ~~Finish `graphics.animation`: override `renderItemInFirstPerson` and route
   BLOCK/EAT/DRINK/BOW through the 1.7 poses.~~ Done: `ItemRendererMixin`
   redirects the first-person item transform (live arm swing for BLOCK and
   EAT/DRINK) and the bow draw so vanilla's pose follows the 1.7 easing curve,
   with the rules unit tested in `FirstPersonAnimsTest`.
5. ~~Add min/max/choices metadata to core `Setting` so the deck stops guessing
   slider ranges from a hard-coded table.~~ Done: `Setting.Range` and
   `Setting.choices` carry the bounds and option lists, and both GUIs read them.
6. ~~Wire the remaining `STUB` modules.~~ Done: name tags, chat timestamps,
   notifications and nick hiding have real hooks, cosmetics draw in world, and
   the six modules with no possible hook were deleted.
