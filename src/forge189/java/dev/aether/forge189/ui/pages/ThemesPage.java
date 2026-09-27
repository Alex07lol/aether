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
            boolean isActive = (theme == activeTheme);

            int bg = isActive ? AetherUi.withAlpha(AetherUi.ACCENT, 0x26) : AetherUi.CARD;
            Mc189Compat.drawRect(cardX, y, cardX + cardW, y + cardH, bg);

            AetherUi.text(font, theme.name(), cardX + 16, y + 10,
                         isActive ? AetherUi.ACCENT : AetherUi.TEXT_SECONDARY);

            int sw = 14;
            int sx = cardX + 16;
            int sy = y + 32;
            AetherUi.drawRect(sx, sy, sx + sw, sy + sw, AetherUi.argb(theme.palette().surface()));
            AetherUi.drawRect(sx + sw + 4, sy, sx + 2 * sw + 4, sy + sw, AetherUi.argb(theme.palette().surfaceSoft()));
            AetherUi.drawRect(sx + 2 * sw + 8, sy, sx + 3 * sw + 8, sy + sw, AetherUi.argb(theme.palette().accent()));
            AetherUi.drawRect(sx + 3 * sw + 12, sy, sx + 4 * sw + 12, sy + sw, AetherUi.argb(theme.palette().text()));
            AetherUi.drawRect(sx + 4 * sw + 16, sy, sx + 5 * sw + 16, sy + sw, AetherUi.argb(theme.palette().glassHighlight()));

            int btnX = cardX + cardW - 100;
            selectBtn.render(font, mouseX, mouseY, btnX, y + 24, isActive ? "SELECTED" : "SELECT");

            y += cardH + 12;
        }
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
    }
}