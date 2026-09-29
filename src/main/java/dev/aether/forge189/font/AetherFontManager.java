package dev.aether.forge189.font;

/**
 * Owns the client's TrueType font sizes.
 * <p>
 * Every size is rasterised on first use, on the render thread, and the result is cached. Building
 * all four sizes up front would rasterise and upload sixteen glyph atlases (four sizes times
 * regular/bold/italic/bold-italic) before the first frame is even drawn, and the UI only ever
 * measures with one of them.
 * <p>
 * A failure is latched. Retrying a failed font build on every frame would re-rasterise the atlases
 * and leak a texture each time, so the manager reports the failure once and answers null from then
 * on, which makes the screens fall back to Minecraft's own font exactly as they already do when the
 * custom font is unavailable.
 */
public final class AetherFontManager {
    private static final String FONT_FAMILY = "SansSerif";

    private static final int SIZE_SMALL = 14;
    private static final int SIZE_MEDIUM = 18;
    private static final int SIZE_LARGE = 24;
    private static final int SIZE_TITLE = 32;

    private static AetherFontManager instance;

    private final GlyphPageFontRenderer[] bySize = new GlyphPageFontRenderer[4];
    private final java.util.Map<Integer, GlyphPageFontRenderer> exactSizes =
        new java.util.LinkedHashMap<Integer, GlyphPageFontRenderer>();
    private boolean failed;
    private String failure = "";

    private AetherFontManager() {
    }

    public static synchronized AetherFontManager instance() {
        if (instance == null) {
            instance = new AetherFontManager();
        }
        return instance;
    }

    /** Small labels and badges. */
    public GlyphPageFontRenderer small() {
        return rendererFor(0, SIZE_SMALL);
    }

    /** The size every screen draws its body text with. */
    public GlyphPageFontRenderer medium() {
        return rendererFor(1, SIZE_MEDIUM);
    }

    /** Section headings. */
    public GlyphPageFontRenderer large() {
        return rendererFor(2, SIZE_LARGE);
    }

    /** Title-screen branding. */
    public GlyphPageFontRenderer title() {
        return rendererFor(3, SIZE_TITLE);
    }

    /**
     * @return the renderer the UI should use, or null when the custom font is not available (or
     *     has no OpenGL texture), so callers keep Minecraft's font instead of drawing nothing.
     */
    public GlyphPageFontRenderer uiFont() {
        GlyphPageFontRenderer renderer = medium();
        return renderer != null && renderer.isUsable() ? renderer : null;
    }

    /** Makes sure the UI font exists. Safe to call from every render pass. */
    public void init() {
        medium();
    }

    public boolean isInitialized() {
        return medium() != null;
    }

    /** @return true when the custom font could not be built; the UI is on Minecraft's font. */
    public boolean hasFailed() {
        return failed;
    }

    /** @return a one-line status for the in-game font diagnostic. */
    public String diagnostic() {
        if (failed) {
            return "custom font unavailable: " + failure;
        }
        GlyphPageFontRenderer renderer = medium();
        if (renderer == null) {
            return "custom font not built yet";
        }
        GlyphPage glyphPage = renderer.regularPage();
        if (glyphPage == null) {
            return "custom font has no glyph page";
        }
        return "atlas " + glyphPage.atlasSize() + "x" + glyphPage.atlasSize()
            + ", glyphs " + glyphPage.glyphCount()
            + ", pages " + renderer.pageCount()
            + ", " + glyphPage.textureSource();
    }

    /**
     * A renderer at an exact point size, rasterised once and cached. The semantic type
     * scale ({@code dev.aether.gui.AetherFont}) asks for whatever point size the current
     * window scale makes correct, so the set of live sizes changes only when the window
     * size or GUI scale does - never per frame.
     *
     * @param fontPt the requested point size, clamped into {@code 8..64}
     * @return the renderer, or null when the custom font is unavailable
     */
    public GlyphPageFontRenderer sized(int fontPt) {
        if (failed) {
            return null;
        }
        int clamped = Math.max(8, Math.min(64, fontPt));
        Integer key = Integer.valueOf(clamped);
        GlyphPageFontRenderer cached = exactSizes.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            GlyphPageFontRenderer renderer = GlyphPageFontRenderer.create(FONT_FAMILY, clamped, true, true, true);
            if (!renderer.isUsable()) {
                failed = true;
                failure = "the glyph atlas has no OpenGL texture (" + renderer.regularPage().textureSource() + ")";
                System.out.println("[Aether] Custom font unavailable: " + failure
                    + "; the UI continues on the Minecraft font.");
                return null;
            }
            exactSizes.put(key, renderer);
            return renderer;
        } catch (Error broken) {
            failed = true;
            failure = broken.getClass().getSimpleName() + ": " + broken.getMessage();
            System.out.println("[Aether] The custom font cannot load because the artifact itself fails to"
                + " link: " + failure + ". This is a build problem, not a font problem - build with"
                + " -PaetherSrgMappings=<mcp-srg.srg> so the jar is remapped for production.");
            throw broken;
        } catch (RuntimeException unavailable) {
            failed = true;
            failure = unavailable.getClass().getSimpleName() + ": " + unavailable.getMessage();
            System.out.println("[Aether] Custom font unavailable after " + failure
                + "; the UI continues on the Minecraft font.");
            return null;
        }
    }

    private GlyphPageFontRenderer rendererFor(int slot, int size) {
        GlyphPageFontRenderer cached = bySize[slot];
        if (cached != null) {
            return cached;
        }
        if (failed) {
            return null;
        }
        try {
            GlyphPageFontRenderer renderer = GlyphPageFontRenderer.create(FONT_FAMILY, size, true, true, true);
            if (!renderer.isUsable()) {
                // The atlas exists but never reached OpenGL; handing it out would draw nothing.
                failed = true;
                failure = "the glyph atlas has no OpenGL texture (" + renderer.regularPage().textureSource() + ")";
                System.out.println("[Aether] Custom font unavailable: " + failure
                    + "; the UI continues on the Minecraft font.");
                return null;
            }
            bySize[slot] = renderer;
            return renderer;
        } catch (Error broken) {
            // An Error is not "this environment has no fonts"; it is "this artifact does not link"
            // - NoSuchMethodError from an unremapped call site, NoClassDefFoundError from a missing
            // runtime. Falling back to Minecraft's font cannot fix that, and swallowing it converted
            // a build failure into a silent visual one: every screen quietly drew vanilla text and
            // nothing pointed at the jar. The one-shot report names the cause and the remap fix;
            // rethrowing ends the render pass the way a linkage error must.
            failed = true;
            failure = broken.getClass().getSimpleName() + ": " + broken.getMessage();
            System.out.println("[Aether] The custom font cannot load because the artifact itself fails to"
                + " link: " + failure + ". This is a build problem, not a font problem - build with"
                + " -PaetherSrgMappings=<mcp-srg.srg> so the jar is remapped for production.");
            throw broken;
        } catch (RuntimeException unavailable) {
            failed = true;
            failure = unavailable.getClass().getSimpleName() + ": " + unavailable.getMessage();
            System.out.println("[Aether] Custom font unavailable after " + failure
                + "; the UI continues on the Minecraft font.");
            return null;
        }
    }
}
