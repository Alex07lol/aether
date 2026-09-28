# Font, UI and rendering-pipeline audit

Pass date: 2026-09-28. Scope: the custom TrueType font pipeline (`GlyphPage`,
`GlyphPageFontRenderer`, `AetherFontManager`), the reflective Minecraft access layer
(`Mc189Compat`), the theme token hub (`AetherUi` / `AetherTheme`), the two screens that were
reported broken (the main menu and the Control Center), the resource packaging and the
production mixin build.

Reference implementation: **Leaf-Client 1.8.9** (`Lefiy/Leaf-Client`,
`leafclient-1.8.9/src/main/java/com/leafclient/font/*`), read as a technical reference only.
It is not vendored into this repository and no Aether architecture was replaced with it.

Companions: `docs/MODULE_LOGIC_AUDIT.md`, `docs/PERFORMANCE_AUDIT.md`,
`docs/CONTROL_CENTER.md`, `docs/ARCHITECTURE.md`.

---

## 1. What was inspected

1. Every Aether file between the screen and the glyph quad: `AetherMainMenuScreen`,
   `AetherClickGuiScreen`, `ui/ControlCenterRenderer`, `ui/pages/*`, `AetherUi`,
   `AetherIcon`, `font/GlyphPage`, `font/GlyphPageFontRenderer`, `font/AetherFontManager`,
   `Mc189Compat` (font, rectangle, texture and GL-state helpers) and the `GL11` shim.
2. Leaf's three font classes and its main-menu mixin, line by line, plus its
   `build.gradle` and `mixins.leafclient.json`.
3. The packaged jar, built from the compiled output plus `src/forge189/resources`, so the
   resource-location claims below are checked against the real archive rather than against
   the source tree.
4. The five mixins and the mixin config, for the mapping requirements a production
   (obfuscated) runtime imposes.

## 2. Aether and Leaf side by side

| Concern | Leaf-Client | Aether (before this pass) | Verdict |
| --- | --- | --- | --- |
| Atlas rasterisation | `FontUtils.createImage`, fixed 512x512 page, ARGB (`TYPE_INT_ARGB`) | `GlyphPage.generateGlyphPage`, page sized from the font metrics, ARGB | Equivalent; Aether's is smaller per size. Left alone. |
| Glyph storage | `CharData[256]` indexed by `char` | `HashMap<Character, Glyph>` | Equivalent. Left alone. |
| Page upload | `new DynamicTexture(image)` | `new DynamicTexture(image)` | Same. |
| Texture id | `tex.getGlTextureId()` - a **direct call**, so ForgeGradle reobfuscates it to `func_110552_b` in the production jar | `Class.forName(...).getDeclaredMethod("getGlTextureId")` inside `catch (Throwable)`, falling back to texture **0** | **Divergent and broken.** See §4. |
| Minification filter | Not set; relies on `TextureUtil` uploading mipmaps | Not set in the `DynamicTexture` path; set to `GL_LINEAR` in the self-upload path added here | Fixed (only the self-upload path needed it). |
| Draw path | `glBegin(GL_TRIANGLES)` + `drawQuad` | `glBegin(GL_TRIANGLES)` + `drawChar` | Equivalent. Kept. |
| Client vertex arrays | Untouched | Untouched | **Hazard.** Fixed, see §5. |
| Matrix / scale | `glPushMatrix` + `scale(0.5)` | `glPushMatrix` + `glScaled(0.5)` | Equivalent. |
| Colour state after a draw | `glColor4f(1,1,1,1)` | `bindTexture(0)` and no colour reset | **Divergent.** Fixed, see §5. |
| Text measurement | Skips a colour code **and only the code** | Skipped the code **and the following character** | **Divergent and wrong.** Fixed, see §4. |
| Font availability | `FontRender` created once, no failure handling | `init()` retried every frame after a failure | Fixed, see §4. |
| Where the font is built | `CustomFont.StartSetup()` from the mod's init | Lazily, on the render thread, from every screen | Aether's is safer (GL context is current). Kept. |
| Main-menu hook | `MixinGuiMainMenu` replaces the vanilla menu | A screen handed to `GuiOpenEvent` | Aether's is safer on a mapped runtime. Kept. |
| Build | ForgeGradle + the Mixin annotation processor | Hand-rolled gradle build with an optional refmap step | See §8. |

## 3. The font pipeline, step by step

```
GlyphPageFontRenderer.drawString
  -> renderString          (argb decode, colour, posX/posY x2)
  -> renderStringAtPos     (matrix push + scale 0.5, blend, alpha, texture enable)
     -> GlyphPage.bindTexture()      <-- step 4, the failure point
     -> GlyphPage.drawChar()         (glBegin, 6 vertices, glTexCoord2f/glVertex2f)
     -> doDraw             (underline/strikethrough via Tessellator, posX advance)
  -> GlyphPage.unbindTexture()
  -> matrix pop
```

Checked step by step:

| Step | State | Note |
| --- | --- | --- |
| 1. `new DynamicTexture(BufferedImage)` | Correct | 1.8.9's constructor copies `getRGB` into `dynamicTextureData` and uploads it as ABGR bytes, which is exactly the `TYPE_INT_ARGB` layout Aether produces. Leaf does the same and works. |
| 2. Texture upload | Correct | Same call as Leaf. |
| 3. Texture id lookup | **Broken** | See §4, root cause 2. |
| 4. Texture binding | Correct once the id is right | `GlStateManager.bindTexture` via `Mc189Compat`, so the state cache stays in sync. |
| 5. `GL_TEXTURE_2D` enable | Correct | Enabled before every draw. |
| 6. Blending | Correct | `enableBlend` + `GL_SRC_ALPHA/GL_ONE_MINUS_SRC_ALPHA`. |
| 7. Alpha test | Correct, and the reason the bug was invisible | Minecraft leaves `GL_ALPHA_TEST` enabled in GUI rendering. A correct atlas passes it; texture 0 does not, so every glyph was discarded while every rectangle kept drawing. |
| 8. Colour | Correct after this pass | `Mc189Compat.color` writes both the GL colour and `GlStateManager`'s cache, so the immediate-mode vertices really get the requested colour. |
| 9. Matrix / scale | Correct | `glScaled(0.5)` with `posX = x * 2`. |
| 10. Atlas size | Correct, now guarded | The packing formula collapses to 0 for a font with unusable metrics; a floor of 64px and a `maxFontHeight` fallback to the point size keep a broken font from throwing instead of degrading. |
| 11. Glyph UVs | Correct, now tested | `x / imgSize`, `(x + w) / imgSize`; the self-test asserts every one of the 256 glyph rects and their derived coordinates stay inside the atlas. |
| 12. Glyph metrics | Correct | `width = bounds.width + 8`, advance `width - 8`, matching Leaf's `- 8.3` kerning convention. |
| 13. Font initialisation | Correct, now latched | Four sizes, each built on first use, and a failure is reported once instead of retried every frame. |

## 4. Root causes

### Root cause 1 - the default theme made the whole UI one light-blue panel

`AetherTheme.defaultTheme()` returned a palette called *Aether Frost* with a near-white
surface `(245,251,255)` and a light-blue raised surface `(210,239,255)`. `AetherUi.syncTheme()`
calls `applyTheme()` on the first frame of every screen, and `applyTheme` derives every token
from the palette - so the dark tokens written in `AetherUi`'s field initialisers were
unreachable, `SURFACE` became near-white, `DECK_BG` became `0xE6D2EFFF` (light blue at 90%
alpha) and the Control Center rendered as one large washed-out panel. This is the exact
rectangle reported in the screenshots, and no screen could opt out of it: the panel was the
theme, not a drawing bug.

Fixed by making the default palette dark (`Aether`, surface `#08090D`, accent `#9B8CFF`),
which is the identity `AetherUi`'s own tokens already describe. The light palettes remain
opt-in theme modules, and `LIGHT_SURFACE` still switches the ink, edge and toggle colours for
them.

### Root cause 2 - the texture id could not be resolved on a production runtime

```
Class.forName("net.minecraft.client.renderer.texture.AbstractTexture")
    .getDeclaredMethod("getGlTextureId")     -> NoSuchMethodException in production
    .getMethod("getGlTextureId")             -> NoSuchMethodException in production
    catch (Throwable ignored) { textureId = 0; }
```

A production 1.8.9 runtime keeps Minecraft's class names but runs methods under their SRG
names, where the same method is `func_110552_b()`. Leaf does not have this problem because
`this.tex.getGlTextureId()` is a direct call, so ForgeGradle reobfuscates it while building.
A reflective lookup cannot be reobfuscated, and the fallback silently bound texture 0 - which
is why the *rectangles* still drew and the *text* did not:

* texture 0 is OpenGL's default 1x1 image, which is not the glyph atlas;
* with `GL_ALPHA_TEST` enabled (as it is in GUI rendering), the sampled alpha fails the test;
* every glyph fragment is discarded, while the Tessellator-drawn rectangles are unaffected.

Fixed with `dev.aether.graphics.TextureIdResolver`, which
1. walks the real class hierarchy, so no declaring class is assumed by name,
2. tries the development name and then the SRG name,
3. falls back to a scan for the only no-argument `func_*` method returning `int` on that
   hierarchy, which is what the getter looks like under any SRG revision,
4. accepts a value only when it is a positive texture name, and
5. reports every attempt it made when nothing resolves.

`GlyphPage` never binds 0. If the id still cannot be read, it uploads the atlas into an
Aether-owned GL texture (`glGenTextures` + `glTexImage2D`, with the non-mipmap
`GL_TEXTURE_MIN_FILTER` that `TextureUtil` would otherwise have supplied), and the diagnostic
line names the reason. The in-game diagnostic screen (F9) shows which path was taken.

### Root cause 3 - text drawn through the generic helper was silently dropped

`Mc189Compat.drawString(Object font, ...)` looked up Minecraft's `(String, int, int, int)`
signature. `GlyphPageFontRenderer` declares `(String, float, float, int)`, so the lookup
returned null and the method returned without drawing anything. `AetherUi.text`,
`AetherUi.tooltip`, `AetherUi.button` and the main menu's footer all route through it, so a
second, independent "text is missing" symptom existed on top of root cause 2, and it was
silent by construction. Fixed by dispatching to the smooth renderer directly in
`drawString`, `drawStringWithShadow` and `stringWidth`.

### Root cause 4 - colour codes corrupted text measurement

`GlyphPageFontRenderer.getStringWidth` consumed the colour-code character *and* the character
after it, so any string containing a colour code measured one glyph narrow. Centred text
drifted right of centre and every tooltip, badge and card sized from a measurement was too
small. Found by the new self-test (`getStringWidth("§cRed")` measured 11 instead of 17) and
fixed to consume only the code, matching Minecraft's own measurement. The same routine no
longer mutates the renderer's bold/italic state while measuring.

### Root cause 5 - a failed font build was retried forever

`AetherFontManager.init()` set `initialized = false` in its catch block and was called from
every render pass, so a font that could not be built was rebuilt - rasterising every atlas and
leaking a texture - on every frame. It also built four sizes (sixteen atlases) when the UI
measures with one. The manager now builds each size on first use, checks that the atlas
actually reached OpenGL before handing it out, and latches a failure with one diagnostic.

## 5. OpenGL state audit

| Helper | Before | After |
| --- | --- | --- |
| `GlyphPage.drawChar` | `glBegin` with no client-state guard; leaves the colour of the last glyph | Disables `GL_VERTEX_ARRAY`, `GL_TEXTURE_COORD_ARRAY` and `GL_COLOR_ARRAY` before the immediate-mode quad, so a preceding Tessellator draw cannot suppress it |
| `GlyphPage.unbindTexture` | `bindTexture(0)` - binds the default image, which is the same mistake as root cause 2 | Restores white colour and keeps the atlas bound, exactly as Minecraft's font renderer does |
| `GlyphPageFontRenderer` | Returned without resetting colour | Colour is restored at the end of every string |
| `GlyphPageFontRenderer` (no texture) | Drew quads that sampled whatever was bound | Draws nothing and reports itself unusable, so the caller falls back to the Minecraft font |
| `Mc189Compat.drawRect` | Sets blend/texture/colour, restores them (both the `Gui.drawRect` path and the Tessellator fallback) | Unchanged - already predictable |
| `Mc189Compat.drawTexture` | Enables texture and blend, resets colour after, but leaves blend enabled | Unchanged; blend-on is the state the next UI primitive wants and every primitive re-establishes what it needs |
| `AetherUi.drawRoundRect` / `drawCircle` | Colours are passed in explicitly and no state is toggled | Unchanged |

One leak was deliberately *not* "fixed": `GlyphPage` keeps the glyph atlas bound instead of
restoring the previous texture. Minecraft's own font renderer does the same, and the
alternative (binding 0) is what caused the original invisible-text bug.

## 6. Main menu background

`AetherMainMenuScreen.drawBackground` draws a 20-step dark gradient with `Mc189Compat.drawRect`
and never touches a texture, so the background was never the missing part - it is the same
shape as the Control Center backdrop. The icons use `Mc189Compat.drawTexture`, which binds
`aether:icon/main_*.png` through the `TextureManager` and then calls
`Gui.drawModalRectWithCustomSizedTexture`; all four referenced icons, plus `background.png`
and the six panorama tiles, are present in the packaged jar (§7). The menu's *text* was missing
for root causes 2, 3 and 4; its background was missing only in the sense that a near-white
default palette made it indistinguishable from the panels on top of it.

## 7. Resources and packaging

Verified on the built archive (`build/jar-verify/aether-0.1.0-forge189.jar`, assembled the same
way `forge189Jar` does: main output + adapter output + `src/forge189/resources` + a manifest
carrying `MixinConfigs`):

| Expectation | Result |
| --- | --- |
| `assets/aether/background.png` | present (1.6 MB) |
| `assets/aether/icon/main_mod_menu.png`, `main_resource_pack.png`, `main_settings.png`, `main_quit.png` | present |
| `assets/aether/icon/search.png`, `check.png`, `cross.png`, `info.png`, ... | present |
| `assets/aether/panorama/panorama_0..5.png` | present |
| `assets/aether/textures/mod/*.png` (module and HUD textures) | present |
| 98 `assets/aether/**` entries in total | matches the source tree |
| `mcmod.info` | present |
| `mixins.aether.json` | present at the jar root |
| `META-INF/MANIFEST.MF` -> `MixinConfigs: mixins.aether.json` | present |
| `net.minecraft.client.renderer.texture.DynamicTexture` id looked up at runtime | see §4 |
| `mixins.aether.refmap.json` | **absent** unless the build is given SRG mappings - see §8 |
| classes | 254, including `dev/aether/AetherClient.class`, `dev/aether/forge189/AetherClickGuiScreen.class`, `dev/aether/forge189/font/*`, `dev/aether/graphics/TextureIdResolver.class` |

Every runtime `ResourceLocation` Aether builds is `new ResourceLocation("aether", path)` with a
path that exists under `assets/aether/`; the icon paths in `AetherMainMenuScreen` and the module
texture paths in `ForgeHudRenderer` were checked against the listing above.

## 8. Mixin and production build findings

`build.gradle` already packages the SRG mapping file for the Mixin processor and passes
`-AoutRefMapFile`/`-AdefaultObfuscationEnv=searge` when `AETHER_SRG_MAPPINGS` (or
`-PaetherSrgMappings`) points at an `mcp-srg.srg`. Without it, a plain build ships
`mixins.aether.json` alone.

That is fine for a development runtime, where member references resolve by MCP name, and it is
the correct minimum-change position - Aether's build was **not** converted to Leaf's ForgeGradle
layout. Two hazards were removed, because the jar can otherwise reach an obfuscated client:

1. `mixins.aether.json` asked for `"required": true` and `"defaultRequire": 1`. A config that
   fails to load, or an injection whose target cannot be resolved, then takes the client down
   at startup. All of Aether's hooks are documented soft hooks, so the config now declares
   `"required": false` and `"defaultRequire": 0`, and every injector and redirect states
   `require = 0` explicitly. A mapping mismatch now degrades to "the module does nothing",
   which is the contract the rest of the adapter already had.
2. `ItemRendererMixin` used three `@Shadow` members. A shadow **cannot** be made optional:
   Mixin fails the whole mixin when it cannot locate the member, and a shadow is only remapped
   when a refmap is present. The mixin now reaches vanilla's `transformFirstPersonItem`,
   `doBowTransformations` and `itemToRender` through `Mc189Compat.call`/`Mc189Compat.read`,
   which try the MCP name and then the SRG name. Both names are tried, so a development
   runtime resolves the MCP name exactly as the shadow did, an obfuscated runtime resolves the
   SRG name, and a runtime where neither matches loses the animation instead of the session.

**Minimum change still required for a shippable obfuscated jar:** generate the refmap by
supplying the mappings, e.g.
`AETHER_SRG_MAPPINGS=/path/to/mcp-srg.srg ./gradlew forge189Jar`, otherwise every injector
resolves by MCP name only and no-ops on an obfuscated client. Documented here rather than
silently forcing the build to fail, because the repository is also built as a development
adapter.

## 9. Dependency table

| Dependency | Used by | Compile / runtime | Provided by MC / Forge | Missing? | Action |
| --- | --- | --- | --- | --- | --- |
| Minecraft 1.8.9 | adapter, mixins | compile (stubs) + runtime | yes | no | none |
| Forge 1.8.9 | `AetherForgeMod`, event bridges, `MixinConfigs` manifest | compile (`AETHER_FORGE_189_JAR`, optional) + runtime | yes | no | none |
| LWJGL 2.9 (`GL11`, `Display`) | `GlyphPage`, `Mc189Compat`, screenshots | **runtime only** | yes | no | The `GL11` shim was extended with the entry points the atlas upload uses (`glGenTextures`, `glTexImage2D`, `glDeleteTextures`, `glGetInteger`, `glEnable`/`glDisable`, `glEnableClientState`/`glDisableClientState`, `glBindTexture`, `glColor4f`, and the texture and array-capability constants). Every one exists in LWJGL 2.9, so the adapter links against the real library. |
| Mixin 0.7.x | the five mixins | compile (Forge ships it) | yes | no | The config no longer requires a load success; refmap generation remains optional. |
| Mixin annotation processor | refmap generation | compile, only when `canGenerateRefmap` | no | only for obfuscated jars | Supplied through `configurations.mixinProcessor` when SRG mappings are given. |
| Gson | config store | compile + runtime | yes (bundled with Minecraft 1.8.9) | no | none |
| Java AWT / `java.awt.image` | glyph rasterisation | runtime (JDK) | no, but no download needed | no | `BufferedImage` + `Graphics2D` work headless; the font test proves it. |
| `java.nio` (direct `IntBuffer`) | atlas self-upload | runtime (JDK) | no | no | none |
| Anything else | - | - | - | no | Leaf's extra dependencies were deliberately not adopted. |

## 10. What was deliberately not done

* The custom font was **not** replaced with Minecraft's font as a workaround. The Minecraft
  font is only used as the documented fallback when the atlas has no texture at all.
* Nothing was disabled, no text rendering was removed, no exception was swallowed into a
  silent `return`, and texture 0 is never bound.
* No UI was redesigned. Layout, the Control Center structure and every screen's geometry are
  untouched; the only visual change is the default palette.
* Leaf's ForgeGradle build was not adopted; only the two hazards in §8 were addressed.
* `GlyphPage.drawChar` kept the immediate-mode path that Leaf also uses (and that works in 1.8.9)
  while the production failure was being fixed, because changing the draw primitive at the same
  time would have made a working reference harder to compare against. It was moved to the
  Tessellator afterwards, once the font rendered - see §13.

## 11. Verification

* `sh scripts/verify.sh` - clean rebuild of core, tests, stubs and adapter, then 27 core test
  suites, `dev.aether.forge189.AetherClickDeckSelfTest` (34 checks) and
  `dev.aether.forge189.font.AetherFontSelfTest` (2238 checks), and a source guard that fails the
  run if an immediate-mode entry point reappears. Ends with `Aether verification passed.`
* `TextureIdResolverTest` covers the development name, the SRG name, an inherited getter, a
  getter that answers 0, the structural scan, a decoy `func_`-shaped name and the failure
  diagnostic.
* `AetherFontSelfTest` covers atlasing, the 256-glyph packing and UV contract, the
  measurement/advance agreement, colour codes adding no width, no style-state leak from a
  measurement, a larger point size producing a larger atlas, and the documented no-op fallback
  when no OpenGL texture exists.
* The jar was assembled and its entries listed (§7).

## 12. Remaining limitations

* **No Forge runtime was available.** No mixin was applied and no frame was rendered, so the
  in-game result is unverified by observation. Every fix is either pure logic covered by a test
  or a call whose name is resolved at runtime rather than assumed.
* The SRG names for `transformFirstPersonItem`, `doBowTransformations` and `itemToRender` are
  best-effort. A wrong name loses the 1.7 animation on an obfuscated client; it cannot crash,
  because the calls are soft.
* The refmap is still only produced when SRG mappings are supplied (§8).
* `ForgeHudRenderer` retains its pre-existing unchecked-operations warning.
* Real verification still requires launching a 1.8.9 client with the mod and pressing F9 in the
  Control Center: the diagnostic screen prints the atlas size, the texture id, whether the id
  came from Minecraft or from Aether's own upload, and the resolved method name.

## 13. The glyph pipeline move, completed

The first pass fixed the font by proving *where* the atlas went and how it was drawn; it left the
draw primitive itself alone, because that was not the bug. This section records moving the drawing
itself, which was the remaining place where UI output depended on OpenGL state rather than on the
data the caller asked for.

### 13.1 What "immediate mode" was actually costing

Three separate things, none of which was the original failure but all of which were latent versions
of it:

1. **Colour lived in GL state.** The glyph quads of the first pass carried a colour (so batching
   was correct), but `renderString` also called `Mc189Compat.color(...)`, and the underline and
   strikethrough quads used a `POSITION` layout with no colour at all - they were filled with
   whatever colour state the last code had left. That made a decoration's colour depend on draw
   order: a coloured run drawn after a reset would still be right, a run drawn after an unrelated
   `color()` call would not. Vanilla 1.8.9 has this shape, which is why it survived review.
2. **The UI's own primitives had the same dependency.** `Mc189Compat.drawRect` (the fallback path
   under `Gui.drawRect`, and the path `AetherUi.drawRoundRect`/`drawCircle` are built out of)
   emitted a `POSITION` quad and relied on the colour state it had just set. `drawTexture`'s
   fallback did the same with `POSITION_TEX`.
3. **One dead immediate-mode helper survived**: `Mc189Compat.drawCircle` still called
   `glBegin(GL_TRIANGLE_FAN)`/`glVertex2f`/`glEnd` through reflection. Nothing referenced it - the
   live circle path is `AetherUi.drawCircle`, which stacks rectangles - so it was reachable only as
   a trap for the next caller, and it could not work in the headless environment where the stub
   deliberately omits those entry points.

### 13.2 What changed

| Site | Before | After |
| --- | --- | --- |
| `GlyphPageFontRenderer.drawDecorations` | `POSITION` quad + current GL colour | `POSITION_COLOR` quad, colour on every vertex |
| `GlyphPageFontRenderer.renderString` / the colour-code branches | set GL colour per string and per code | no colour-state write at all; the colour is an argument that reaches the vertices |
| `Mc189Compat.drawRect` fallback | `POSITION` + `color()` | `POSITION_COLOR`, colour per vertex |
| `Mc189Compat.drawTexture` fallback | `POSITION_TEX` + white state | `POSITION_TEX_COLOR`, white at the vertex |
| `Mc189Compat.drawCircle` | reflective `glBegin`/`glVertex2f`/`glEnd` | deleted (unreferenced; `AetherUi.drawCircle` is the live path) |
| `Mc189Compat.color` | `GlStateManager.color`, else `glColor4f` | `GlStateManager.color` only, with a one-shot report if it cannot be reached |

The colour state is now written in exactly one place per string: white, once, when the string ends.
That is a deliberate normalisation for the vanilla-font fallback and any HUD code that still sets a
colour for its own `POSITION` quads - it is no longer something the custom font reads.

### 13.3 A real bug found while wiring this

`Mc189Compat.invokeStatic` returns the method's value, which is `null` for **every void method** -
and nearly every state call in that class is void. Callers written as
`if (invokeStatic(...) == null) { fallback }` therefore ran the fallback *always*, which was
harmless only because the fallbacks happened to do the same thing (`GlStateManager.enableBlend`
then `glEnable(GL_BLEND)`, and so on). `color` was the first caller that wanted to distinguish the
two cases - it drops the GL fallback and reports the failure - so the ambiguity was removed with a
separate `invokeStaticVoid` that answers "was the method reached" directly. Attaching the
diagnostic to the old `== null` test would have printed a false warning on the first colour call of
every session.

### 13.4 How it is verified without a GPU

The stubs record what the renderer emitted, so the property is asserted rather than inspected:

* `AetherFontSelfTest.decorationsCarryTheirOwnColour` - a `§m` and a `§n` run each produce a
  `POSITION_COLOR` quad of four vertices with no texture coordinates, every vertex carrying the run
  colour, positioned as a horizontal bar at the pen position.
* `AetherFontSelfTest.textIgnoresColourStateAndLeavesItWhite` - seals the state purple through
  `Mc189Compat.color`, draws white text, and asserts the vertices are white *and* that the state is
  white afterwards. This is the exact failure the class javadoc now promises cannot happen: text
  can neither be tinted by the previous draw nor tint the next one.
* `GlStateManager` in the stub records the last colour it was given, which is the only observable GL
  state the move left behind.
* `scripts/verify.sh` fails the run if `glBegin`, `glEnd(`, `glVertex2f`/`glVertex3f`,
  `glTexCoord2f` or `glColor4f` appear anywhere in the core or adapter sources outside a comment
  (comments are excluded; the sources name these entry points when explaining why they are gone).
  `GL_TRIANGLE_FAN` is not matched because, as a Tessellator mode, it is a legitimate primitive.

Self-test counts after the move: font 2238 checks, Control Center deck 34 checks, 27 core suites,
`Aether verification passed.`

### 13.5 What did not change

The vertex layouts, the atlas, the packing, the 0.5-scaled matrix, the texture binding and the
colour-code semantics are untouched, so the visual output is identical by construction; the
difference is that no part of it is read from GL state. Minecraft 1.8.9's own `FontRenderer` draws
its glyphs with `glBegin(GL_TRIANGLE_STRIP)` and its decorations with a `POSITION` Tessellator
batch, so the custom renderer is now *less* state-dependent than the vanilla one it shadows.
