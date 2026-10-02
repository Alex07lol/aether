package dev.aether.menu.impl;

import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.theme.ThemeDefinition;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The Appearance category: the theme wardrobe. Swatches of 36x36 (radius 6) show
 * every registered palette, the worn one carries a 1.4px accent outline, and the
 * header line names the active theme. Selecting wears the palette immediately and
 * persists through the client's save path - themes are configuration, never modules.
 */
public final class AppearanceCategory extends MenuCategory {

    private static final float SWATCH = 36.0F;
    private static final float STEP_X = 46.0F;

    public AppearanceCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
    }

    @Override
    public String title() {
        return "Appearance";
    }

    @Override
    public void draw(double mx, double my) {
        ThemeDefinition[] themes = client.themes().themes();
        setContentHeight(120.0F);

        float x = innerX();
        float y = contentY() + 20.0F - scroll;

        AetherFont.draw(11.0F, dev.aether.forge189.font.AetherFontManager.Face.SEMIBOLD, "Theme",
            x, y, UiTheme.text());
        y += 24.0F;

        for (int i = 0; i < themes.length; i++) {
            ThemeDefinition theme = themes[i];
            float sx = x + i * STEP_X;
            boolean active = theme.id().equals(client.themes().activeId());
            boolean hot = mx >= sx && mx < sx + SWATCH && my >= y && my < y + SWATCH;

            dev.aether.theme.ThemePalette palette = theme.palette();
            int surface = 0xFF000000 | (palette.surface().red() << 16) | (palette.surface().green() << 8) | palette.surface().blue();
            int soft = 0xFF000000 | (palette.surfaceSoft().red() << 16) | (palette.surfaceSoft().green() << 8) | palette.surfaceSoft().blue();
            int accent = 0xFF000000 | (palette.accent().red() << 16) | (palette.accent().green() << 8) | palette.accent().blue();

            if (active) {
                UiCanvas.outline(sx - 1.4F, y - 1.4F, SWATCH + 2.8F, SWATCH + 2.8F, 6.0F + 1.4F,
                    UiTheme.accent(), 1.4F);
            }
            UiCanvas.roundRect(sx, y, SWATCH, SWATCH, 6.0F, surface);
            UiCanvas.roundRect(sx + 5.0F, y + 5.0F, SWATCH - 10.0F, SWATCH - 10.0F, 4.0F, soft);
            UiCanvas.circle(sx + SWATCH / 2.0F, y + SWATCH / 2.0F, 5.0F, accent);
            if (hot && !active) {
                UiCanvas.outline(sx, y, SWATCH, SWATCH, 6.0F, UiTheme.withAlpha(UiTheme.text(), 120), 1.0F);
            }
            AetherFont.drawCentered(7.5F, AetherFont.trim(7.5F, theme.name(), 44),
                sx - 4.0F, y + SWATCH + 6.0F, SWATCH + 8.0F,
                active ? UiTheme.text() : UiTheme.textSoft());
        }

        // The active theme's summary.
        ThemeDefinition active = client.themes().active();
        float summaryY = y + SWATCH + 26.0F;
        AetherFont.draw(11.0F, dev.aether.forge189.font.AetherFontManager.Face.SEMIBOLD,
            active.name(), x, summaryY, UiTheme.text());
        AetherFont.draw(7.5F, "This palette colours the menu, the HUD and every screen surface.",
            x, summaryY + 16.0F, UiTheme.textFaint());
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return true;
        }
        ThemeDefinition[] themes = client.themes().themes();
        float x = innerX();
        float y = contentY() + 20.0F - scroll + 24.0F;
        for (int i = 0; i < themes.length; i++) {
            float sx = x + i * STEP_X;
            if (mx >= sx && mx < sx + SWATCH && my >= y && my < y + SWATCH) {
                if (!themes[i].id().equals(client.themes().activeId())) {
                    client.themes().select(themes[i].id());
                    saveQuietly();
                }
                return true;
            }
        }
        return true;
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    public void debugSelect(int index) {
        ThemeDefinition[] themes = client.themes().themes();
        if (index >= 0 && index < themes.length) {
            client.themes().select(themes[index].id());
            saveQuietly();
        }
    }
}
