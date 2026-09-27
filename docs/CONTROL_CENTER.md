# Aether Control Center

The Aether Control Center is the client's primary click GUI. It replaces the old
*Click Deck* navigation model (a ribbon of view chips) with a real two-level layout:
a persistent **sidebar of pages** and a **main area** that each page fills. The old
deck's body — one row per module, inline accordions, keyboard-first input, shared
layout/hit-testing, viewport culling — is kept intact.

Everything in this document describes code that compiles and runs in the headless
self-test (`AetherClickDeckSelfTest`, 33 checks).

---

## 1. Screens

The screen class is `dev.aether.forge189.AetherClickGuiScreen`. It is the entry point
the mod-menu key opens, and it owns:

- the render pass (`render`),
- the single layout computation (`computeLayout`),
- the click path (`click`),
- the keyboard path (`handleKey`),
- section/page drawing (`drawSidebar`, `drawRows`, `drawPageRows`).

## 2. The model in core

The navigation logic is deliberately **not** in the Forge adapter. Three small classes
in `dev.aether.ui` (core, Minecraft-free, unit-tested) carry it:

| Class | Responsibility |
| --- | --- |
| `ControlCenterSection` | The six pages (Modules / Profiles / Themes / Cosmetics / Screenshots / Settings), their labels, ordering and wrap-around cycling. |
| `ControlCenterState` | The state machine: current section, menu state, open settings module, expanded rows, selection, scroll, active filter chip and the focused control. All mutations go through named transitions. |
| `ControlFocus` | What owns the mouse/keyboard inside a page: idle, slider drag, colour palette, keybind capture, or text edit — one object instead of four nullable fields. |

Why in core? The same state machine is what the config
(`preference.open_section`), the sidebar, the keyboard cycle and the tests all read.
A future second surface (e.g. an in-game quick menu) can reuse the exact navigation
rules without dragging Swing–style screen code with it.

## 3. State machine

```
                +-------------------------------------+
                |  ControlCenterState.MenuState       |
                +-------------------------------------+
   showSection  |  BROWSING                           |
   ------------>|  (module list, or page rows)        |
                |                                     |
   click search |  SEARCHING                          |
   or press '/' |  (query owns the keyboard)          |
   ------------>|                                     |
                |  MODULE_SETTINGS                    |
   openModule   |  (a single module's accordion)      |
   Settings(id) |                                     |
   ------------>|  GLOBAL_SETTINGS                    |
                |  (client preference rows)           |
                +-------------------------------------+
```

Every transition is a method on `ControlCenterState`, and each one drops the state
that would be inconsistent with the new one:

- `showSection(next)` clears the open module, the focus, the selection and the scroll.
- `beginSearch()` leaves a settings page automatically.
- `endSearch()` returns to browsing — or to `GLOBAL_SETTINGS` when the section is
  Settings, so search never strands the user on a page whose content vanished.
- `openModuleSettings(id)` refuses blank ids and always names the module it opens.
- `consistent()` asserts the invariant: a settings page always has a module, and a
  module is never named when no settings page is open.

The four booleans the old screen kept (`searchFocused` on top of a module id on top
of a palette on top of a capture) could express combinations like *keybind capture
running while a text field is being edited*. Those combinations are no longer
representable: focusing a new control replaces `ControlFocus` wholesale.

## 4. Sidebar

The sidebar is drawn from `ControlCenterSection.ordered()`:

```
Modules  Profiles  Themes  Cosmetics  Screenshots  Settings
```

Under the sections sits a **Filters** block: `All / Live / Favorites` plus one chip
per non-empty module category. Filters are the *same* composable model the search
uses (`ModuleSearch`): a filter chip, a query, and the Live/Favorites switches all
compose, so `Favorites + PvP + "reach"` works.

Hit rectangles for the sidebar are registered during **layout**, not painting. The
click path runs `computeLayout` first, so a map cleared in layout and refilled in
paint would hand every click an empty map — that bug was found by the self-test and
fixed by moving registration into `layoutSidebar()`.

## 5. Sections

| Section | Content |
| --- | --- |
| **Modules** | The full module list: search, filters, accordions, per-module reset, HUD-editor jump for HUD modules. |
| **Profiles** | Every saved profile (name, enabled-module count), a name field, and Save / Apply / Delete actions. Backed by `ProfileStore`. |
| **Themes** | The same module machinery filtered to the `THEMES` category; enabling one module switches the palette through the registry's exclusive-group rule. |
| **Cosmetics** | The same module machinery filtered to `COSMETICS`; the spine's HUD-editor / cosmetics buttons stay available. |
| **Screenshots** | The async screenshot list (name, timestamp, size), Take Screenshot, Open Folder and Delete per shot. No pixels are decoded for the list — metadata only. |
| **Settings** | Client-wide preferences (`save_on_close`, `show_tooltips`, plus the remembered open section) stored under `preference.*`. |

There are no placeholder pages: a section exists only because it has real content and
a real backing store.

## 6. Search and caching

Search goes through the cached `ModuleSearch` pipeline:

- `source(...)` hands it the registry snapshot for the active section
  (all modules, or only Themes / Cosmetics),
- `query(...)`, `category(...)`, `liveOnly(...)`, `favoritesOnly(...)` are the
  composable filters,
- `results()` recomputes **only when an input actually changed** (`isDirty`),
- scoring ranks name > id > description > category, with a contiguity bonus — the
  same rules the old screen inlined per frame.

Because section sources change (Modules vs Themes vs Cosmetics) the screen re-syncs
`source` on section switches; identical snapshots are ignored by `ModuleSearch`, so
switching back and forth does not re-score anything.

## 7. Shared layout / hit-testing

The invariant from the deck is unchanged and load-bearing:

```
computeLayout(w, h)
   ├── boxes[], sidebarHits[], pageRowHits[], pageActionHits[], spineButtons[]
   ├── renderer  (drawRows / drawPageRows / drawSidebar / drawSpine)
   └── hit-testing (click / clickPageRow)
```

- one `computeLayout` per frame per consumer,
- the click path calls `computeLayout` again before touching any rectangle,
- the module list clips (`pushClip`/`popClip`) and culls rows outside the viewport,
- a click outside the list column can never land on a row that happens to share its
  y range.

## 8. Keyboard model

| Key | Action |
| --- | --- |
| `/` | Focus the search field |
| `Esc` | Clear search → unfocus → close the menu (in that order) |
| `Up` / `Down` | Move selection |
| `PageUp` / `PageDown` | Move selection by five |
| `Home` / `End` | Jump to the first / last row |
| `Enter` | Toggle the selected module (or commit a text field / close search) |
| `Left` / `Right` | Collapse / expand the selected row |
| `Space` | Expand / collapse |
| `Tab` / `Shift+Tab` | Cycle sections (wraps in both directions) |
| `E` / `C` | Expand all / collapse all |
| `R` | Reset the selected module's settings |

Selection is always clamped to the current list, and `scrollToSelected` keeps the
selection visible after filtering.

## 9. Responsiveness

- Below a deck width of 560 the sidebar is hidden and the list takes the full width;
  keyboard section cycling (`Tab`) still reaches every page.
- Below an inner width of 430 the telemetry spine is hidden.
- The search field, the section list and the controls are never removed at any width;
  only secondary metadata (category tags, "N set" captions, per-category bars) yields.

## 10. Persistence

- Every change flows through `AetherClient.save()` — the same single save path as
  before. `ClientPreferences` (`preference.*`) and `ProfileStore`
  (`profile.<name>.<key>`) write into the same config document as modules.
- The last open section is stored in `preference.open_section` and normalised on
  load; an unknown value falls back to Modules.
- Profiles apply through `ModuleRegistry.applyConfig`, so restoring a profile runs
  the same normalisation path as loading the config file — a profile can never
  restore an out-of-range value.

## 11. Extending

- **New page:** add a constant to `ControlCenterSection`, a case in
  `layoutPageRows` / `drawPageRows` / `clickPageRow` (only if it needs custom rows),
  and sidebar registration happens automatically.
- **New module category:** add the enum constant to `ModuleCategory`; the sidebar
  chip, the label and the filter all derive from it.
- **New control type:** add the `Setting.SettingType`, one branch in `controlRect`
  (geometry), `drawControl` (paint) and `handleControlClick` (input). The metadata
  (range, choices, default) stays on the setting — the screen never invents bounds.

## 12. Testing

- `AetherClickDeckSelfTest` (adapter, headless): renders every section, flips a
  module with Enter and by clicking its switch, expands accordions and exercises
  every control type, searches, cycles sections with Tab, clicks sidebar sections
  and preference rows, saves/applies/deletes a profile through the page buttons,
  verifies theme switching repaints the shared tokens, and checks a narrow viewport
  (320×240) still renders.
- `ControlCenterStateTest` (core): state transitions, invariants, clamping, filter
  keys and the consistency rule.
- `ModuleSearchTest` (core): filtering, ranking, filter composition and cache
  invalidation rules.
