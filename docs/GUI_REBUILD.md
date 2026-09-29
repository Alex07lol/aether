# Aether In-Game GUI Rebuild

Status: implemented. This document records the architecture decisions of the 2026-09-29
rebuild: what was taken from the Leaf Client 1.8.9 GUI, what was deliberately done
differently, and what was deleted.

References:

- Leaf Client (structural baseline): https://github.com/Lefiy/Leaf-Client
  (`leafclient-1.8.9/src/main/java/com/leafclient/screen/**`). Leaf is GPLv3; the
  adapted files carry the attribution header required by that license.
- Aether core (`dev.aether.*`) is unchanged in spirit: the GUI keeps being a thin
  consumer of `ModuleRegistry`, `Setting`, `CosmeticLibrary`, `HudLayout`,
  `ClientPreferences` and `ProfileStore`.

## What was wrong with the old GUI

The previous implementation (the "Control Center" deck plus the legacy ModMenu) had:

- two competing GUI implementations (`AetherClickGuiScreen` deck, `AetherModMenuScreen`
  classic list) plus three screens that could never be reached at all;
- three hand-rolled mouse-to-GUI coordinate conversions and four rounded-rect
  implementations;
- two scissor code paths, one of them dead;
- index-based scrolling where a filter change silently desynced rendering and hit-testing;
- GL state handled per-call-site with no discipline (the reported alpha bleed, category
  chips painting through cards, and text at wrong positions);
- heavy dead ballast (unused FPS sparkline, dead Box/Rect geometry, dead stubs,
  an unused `ui/theme` package, duplicated icon painters).

Per the brief, the old presentation was replaced instead of patched.

## Leaf 1.8.9 structure that was kept (as concepts, not copies)

| Leaf | Aether |
| --- | --- |
| One `GuiScreen` per section (`ModSettings`, `CosmeticSettings`, `ModPosSettings`, `ClientSettings`) | `AetherModScreen`, `AetherCosmeticScreen`, `AetherHudEditorScreen`, `AetherClientSettingsScreen`, all extending one shared `AetherGuiScreen` shell |
| `UIBase` render / onMouseMove / onMouseClick lifecycle | `dev.aether.gui.core.UiComponent` with render, mouseMove, mouseClick, mouseRelease, keyTyped, onResize, dispose |
| `SystemButton` / `ModButton` / `CosmeticButton` / `SelectButton` / `ToggleButton` / `TextBox` | `gui/components/Button`, `Toggle`, `Slider`, `ChoicePill`, `TextField` — all procedural, no texture dependencies |
| `ScrollBar` | `gui/ScrollContainer` (see below: real clipping, not index paging) |
| `ScaleFixer` (design space -> display pixels) | `gui/GuiScale` (see below: single converter, no integer truncation) |
| `CosmeticSettings` dedicated cosmetics screen with category selector + large player preview | `AetherCosmeticScreen` with the same shape; preview in `gui/preview/PlayerPreview` |
| `CustomFont` centralized renderer | `AetherFontManager` (existing) behind the new semantic `gui/AetherFont` facade (TITLE / SECTION / BODY / SMALL / CAPTION) |
| Cosmetic state updated immediately on click, renderer reads it next frame | unchanged: the screen calls `CosmeticLibrary.select(id)`, `ForgeCosmeticRenderer` keeps reading `effective(type)` |

## What was deliberately done differently from Leaf

1. **Scaling.** Leaf's `ScaleFixer` mixes `Toolkit` screen size with the window size,
   truncates to `int` per call, and relies on every component remembering to convert. The
   Aether `GuiScale` keeps a design canvas 1080 units tall whose width follows the window
   aspect (exactly 1920x1080 at 16:9), converts with `double` math in one place, and
   converts mouse coordinates back with the same object. Components are laid out and
   hit-tested purely in design units. GUI scale, high-DPI and windowed/fullscreen are
   inherited from `ScaledResolution`, which the design canvas sits on.
2. **Scrolling.** Leaf's `ScrollBar` pages whole rows by index, so partially visible rows
   and hit-testing disagree. Aether's `ScrollContainer` clips with a real scissor rect,
   translates children by a clamped float offset, culls children fully outside the
   viewport from both rendering and input, and converts mouse coordinates into content
   space with the same offset that rendering used.
3. **Input hierarchy.** Leaf forwards clicks to every visible component and lets each
   component test containment. Aether routes input top-down: the shell first (nav, search,
   footer), then the scroll viewport only when the cursor is inside it, then the topmost
   component under the cursor. A click cannot fall through a card to whatever is behind it.
4. **No database/GUI coupling.** Leaf's `CosmeticSettings` writes to its database inside
   the click handler. Aether's cosmetics screen only touches `CosmeticLibrary` and relies
   on the existing save path (`client.save()`).
5. **Visual identity.** Leaf's green texture-backed surfaces are replaced by Aether's
   dark glass tokens (`AetherUi`), restrained purple accent, and procedurally drawn
   rounded rectangles, borders and controls. No Leaf textures, package names, URLs or
   branding are used.

## New architecture (`dev.aether.gui`)

```
dev.aether.gui
├── AetherGui            screen registry + open() helpers + branding painter
├── ScreenTab            MODULES / COSMETICS / HUD / SETTINGS (nav model)
├── GuiScale             the single design-space converter
├── AetherFont           semantic type scale over AetherFontManager
├── core/
│   ├── UiComponent      component lifecycle (Leaf UIBase equivalent)
│   ├── ScrollContainer  viewport clip + translate + culling + clamped offset
│   └── ScreenShell      shared header/nav/footer painter used by all four screens
├── components/          Button, Toggle, Slider, ChoicePill, TextField, ScrollBar(view)
├── screens/
│   ├── AetherGuiScreen  base screen: shell, input routing, ESC model, GL guard
│   ├── AetherModScreen  search + All/Live/Favorites + category chips + cards + settings panel
│   ├── AetherCosmeticScreen  category selector + entries + import + player preview
│   ├── AetherHudEditorScreen drag/snap/scale/opacity editor (logic ported from the old screen)
│   └── AetherClientSettingsScreen  preferences, profiles, save/screenshot actions
└── preview/PlayerPreview  isolated player render (own matrix/projection state, full restore)
```

State remains in the managers (no GUI-owned truth): module state in `ModuleRegistry`,
cosmetic slots in `CosmeticLibrary`, HUD positions in `HudLayout` (now persisted under
`hud.<id>.*`), preferences in `ClientPreferences`, profiles in `ProfileStore`.

### Input and focus model

- Keyboard goes to the focused control: search field, text setting, or keybind capture,
  expressed with the existing tested `dev.aether.ui.ControlFocus` machine.
- ESC: closes a module settings panel or defocuses search first, then returns to the
  game. Navigation between tabs swaps `GuiScreen` instances (fresh components, no stale
  scroll), while all durable state lives in the managers.
- The mouse wheel scrolls the viewport under the cursor; over the player preview it
  zooms; over the HUD editor it scales the selected element.

### GL discipline

Every primitive goes through `Mc189Compat`, which resets colour to white and restores
texture/blend after each draw. `ScrollContainer` opens and closes its scissor in a
`finally`. `PlayerPreview` wraps the entity render in push/pop matrix, its own scissor,
standard-item lighting teardown, rescale-normal disable and lightmap texture unit
restore, mirroring the vanilla `GuiInventory.drawEntityOnScreen` teardown sequence.

## Removed

- `AetherClickGuiScreen` + `forge189/ui/**` (ControlCenter layout/renderer/input, pages,
  toasts, metrics, icons, duplicate components, the dead `ui/theme` package)
- `AetherModMenuScreen` (classic three-panel manager)
- `AetherCosmeticsScreen` (legacy import screen; import lives on the new cosmetics screen)
- `AetherQuickNavScreen`, `AetherAccountManagerScreen`, `AetherInfoScreen`
- `dev.aether.ui.ControlCenterState` / `ControlCenterSection` and their test
  (`ModuleSearch` and `ControlFocus` stay; they are screen-agnostic)
- the ModMenu wheel-forwarding branch in `ForgeGuiEventBridge`

## Licensing

Leaf Client is GPLv3. The components whose structure is adapted from Leaf
(`GuiScale`, the screen set, the cosmetics preview layout) carry a header noting the
derivation. No Leaf source file is copied verbatim, no Leaf asset, texture, font or
branding is used, and this document preserves the attribution the license requires.
