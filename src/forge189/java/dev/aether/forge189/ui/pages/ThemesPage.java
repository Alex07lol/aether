package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.module.ClientModule;
import dev.aether.theme.AetherTheme;
import dev.aether.theme.ThemeModule;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherButton;
import dev.aether.forge189.ui.AetherMetrics;
import java.util.List;
import java.util.ArrayList;

/**
 * Themes page – visual theme cards with palette previews.
 */
public final class ThemesPage extends Page {

    private final AetherButton selectBtn = new AetherButton("Select", 0, 0, 80, 22);

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        AetherTheme activeTheme = screen.getClient().theme();

        // Get theme modules from the client
        List<AetherTheme> themes = new ArrayList<>();
        for (ClientModule module : screen.getClient().modules().all()) {
            if (module instanceof ThemeModule) {
                ThemeModule themeModule = (ThemeModule) module;
                themes.add(AetherTheme.of(themeModule.metadata().name(), themeModule.palette()));
            }
        }

        int y = layout.listY + 8;
        AetherUi.text(font, "THEMES", layout.listX + 8, y, AetherUi.TEXT_DISABLED);
        y += 30;

        for (AetherTheme theme : themes) {
            int cardX = layout.listX + 8;
            int cardW = layout.listW - 16;
            int cardH = 70;
            boolean isActive = theme.name().equals(activeTheme.name());

            // Card background with hover effect
            boolean hover = mouseX >= cardX && mouseX <= cardX + cardW &&
                            mouseY >= y && mouseY <= y + cardH;
            int bg = isActive ? AetherUi.withAlpha(AetherUi.ACCENT, 0x26) :
                       hover ? AetherUi.lerpColor(AetherUi.CARD, AetherUi.ACCENT, 0.04f) : AetherUi.CARD;
            Mc189Compat.drawRect(cardX, y, cardX + cardW, y + cardH, bg);

            // Active indicator
            if (isActive) {
                Mc189Compat.drawRect(cardX, y, cardX + 3, y + cardH, AetherUi.ACCENT);
            }

            // Theme name
            AetherUi.text(font, theme.name(), cardX + 16, y + 10,
                         isActive ? AetherUi.ACCENT : AetherUi.TEXT_PRIMARY);

            // Active badge
            if (isActive) {
                AetherUi.text(font, "ACTIVE", cardX + 16, y + 24, AetherUi.ACCENT);
            }

            // Palette preview - 5 color swatches
            int sw = 14;
            int sx = cardX + 16;
            int sy = y + 32;
            drawPaletteSwatch(sx, sy, sw, theme.palette().surface(), "Surface");
            drawPaletteSwatch(sx + sw + 4, sy, sw, theme.palette().surfaceSoft(), "Soft");
            drawPaletteSwatch(sx + 2 * sw + 8, sy, sw, theme.palette().accent(), "Accent");
            drawPaletteSwatch(sx + 3 * sw + 12, sy, sw, theme.palette().text(), "Text");
            drawPaletteSwatch(sx + 4 * sw + 16, sy, sw, theme.palette().glassHighlight(), "Glass");

            // Select button
            int btnX = cardX + cardW - 100;
            selectBtn.render(font, mouseX, mouseY, btnX, y + 24, isActive ? "SELECTED" : "SELECT");

            y += cardH + 12;
        }
    }

    private void drawPaletteSwatch(int x, int y, int size, dev.aether.theme.ColorRgb color, String label) {
        int argb = AetherUi.argb(color);
        Mc189Compat.drawRect(x, y, x + size, y + size, argb);
        // Border
        Mc189Compat.drawRect(x, y, x + size, y + 1, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        Mc189Compat.drawRect(x, y + size - 1, x + size, y + size, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        Mc189Compat.drawRect(x, y, x + 1, y + size, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
        Mc189Compat.drawRect(x + size - 1, y, x + size, y + size, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33));
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        AetherTheme activeTheme = screen.getClient().theme();

        // Get theme modules from the client
        List<AetherTheme> themes = new ArrayList<>();
        for (ClientModule module : screen.getClient().modules().all()) {
            if (module instanceof ThemeModule) {
                ThemeModule themeModule = (ThemeModule) module;
                themes.add(AetherTheme.of(themeModule.metadata().name(), themeModule.palette()));
            }
        }

        int y = layout.listY + 8 + 30;
        for (AetherTheme theme : themes) {
            int cardX = layout.listX + 8;
            int cardW = layout.listW - 16;
            int cardH = 70;
            boolean isActive = theme.name().equals(activeTheme.name());

            // Select button hit test
            int btnX = cardX + cardW - 100;
            if (mouseX >= btnX && mouseX <= btnX + 80 &&
                mouseY >= y + 24 && mouseY <= y + 24 + 22) {
                if (!isActive) {
                    // Enable the theme module
                    for (ClientModule module : screen.getClient().modules().all()) {
                        if (module instanceof ThemeModule) {
                            ThemeModule tm = (ThemeModule) module;
                            if (tm.metadata().name().equals(theme.name())) {
                                screen.getClient().modules().setEnabled(tm.metadata().id(), true);
                                screen.writeLastChange("Theme changed to " + theme.name());
                                screen.getToasts().push("Theme set to " + theme.name(), AetherUi.ACCENT);
                                break;
                            }
                        }
                    }
                }
                return;
            }

            // Click anywhere on card to select
            if (mouseX >= cardX && mouseX <= cardX + cardW &&
                mouseY >= y && mouseY <= y + cardH) {
                if (!isActive) {
                    for (ClientModule module : screen.getClient().modules().all()) {
                        if (module instanceof ThemeModule) {
                            ThemeModule tm = (ThemeModule) module;
                            if (tm.metadata().name().equals(theme.name())) {
                                screen.getClient().modules().setEnabled(tm.metadata().id(), true);
                                screen.writeLastChange("Theme changed to " + theme.name());
                                screen.getToasts().push("Theme set to " + theme.name(), AetherUi.ACCENT);
                                break;
                            }
                        }
                    }
                }
                return;
            }

            y += cardH + 12;
        }
    }
}