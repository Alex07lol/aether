package dev.aether.forge189.font;

public final class AetherFontManager {
    private static AetherFontManager instance;

    public GlyphPageFontRenderer fontSmall;
    public GlyphPageFontRenderer fontMedium;
    public GlyphPageFontRenderer fontLarge;
    public GlyphPageFontRenderer fontTitle;
    private boolean initialized;

    private AetherFontManager() {
    }

    public static synchronized AetherFontManager instance() {
        if (instance == null) {
            instance = new AetherFontManager();
        }
        return instance;
    }

    public void init() {
        if (initialized) return;
        try {
            String fontName = "SansSerif";
            fontSmall = GlyphPageFontRenderer.create(fontName, 14, true, true, true);
            fontMedium = GlyphPageFontRenderer.create(fontName, 18, true, true, true);
            fontLarge = GlyphPageFontRenderer.create(fontName, 24, true, true, true);
            fontTitle = GlyphPageFontRenderer.create(fontName, 32, true, true, true);
            initialized = true;
        } catch (Throwable t) {
            // Fall back gracefully if system fonts are restricted
            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
