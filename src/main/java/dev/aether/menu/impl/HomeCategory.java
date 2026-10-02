package dev.aether.menu.impl;

import dev.aether.gui.AetherFont;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.MenuCategory;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;

/**
 * The Home category: a compact dashboard - the client wordmark, three stat cards
 * (modules enabled, worn theme, active profile) and two shortcut buttons. Everything
 * on it is read from the live managers; nothing is decorative.
 */
public final class HomeCategory extends MenuCategory {

    public HomeCategory(AetherMenuScreen screen, dev.aether.AetherClient client) {
        super(screen, client);
    }

    @Override
    public String title() {
        return "Home";
    }

    @Override
    public void draw(double mx, double my) {
        float x = innerX();
        float y = contentY() + 18.0F - scroll;
        setContentHeight(170.0F);

        AetherFont.draw(11.0F, dev.aether.forge189.font.AetherFontManager.Face.SEMIBOLD, "Overview",
            x, y, UiTheme.text());
        y += 26.0F;

        // Stat cards: 120x56, three across the content width.
        float cardW = 120.0F;
        float cardH = 56.0F;
        float gap = (innerW() - 30.0F - cardW * 3.0F) / 2.0F;

        int enabled = 0;
        for (ClientModule module : client.modules().userVisible()) {
            if (module.state() == ModuleState.ENABLED) {
                enabled++;
            }
        }

        drawStat(x, y, cardW, cardH, "MODULES ON", String.valueOf(enabled),
                enabled + " of " + client.modules().userVisible().size() + " active", mx, my);
        drawStat(x + cardW + gap, y, cardW, cardH, "THEME",
            client.themes().active().name(), "worn palette", mx, my);
        String profile = client.profiles().names().isEmpty() ? "None" : "(saved)";
        drawStat(x + (cardW + gap) * 2.0F, y, cardW, cardH, "PROFILES",
            String.valueOf(client.profiles().size()), profile.equals("(saved)")
                ? "saved configurations" : "no profiles yet", mx, my);

        // Shortcuts.
        float buttonY = y + cardH + 14.0F;
        drawShortcut(x, buttonY, 120.0F, UiIcon.MODULES, "Browse modules", mx, my);
        drawShortcut(x + 130.0F, buttonY, 120.0F, UiIcon.EDIT_HUD, "Edit the HUD", mx, my);

        AetherFont.draw(7.5F, "Aether 0.1.0 - press Right Shift anywhere to open this menu",
            x, buttonY + 34.0F, UiTheme.textFaint());
    }

    private void drawStat(float x, float y, float w, float h, String label, String value,
                          String sub, double mx, double my) {
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + h;
        UiCanvas.roundRect(x, y, w, h, 8.0F, hot ? UiTheme.cardHover() : UiTheme.withAlpha(UiTheme.card(), 235));
        AetherFont.draw(7.5F, label, x + 10.0F, y + 9.0F, UiTheme.textFaint());
        AetherFont.draw(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
            dev.aether.gui.AetherFont.trim(13.0F, dev.aether.forge189.font.AetherFontManager.Face.MEDIUM,
                value, (int) (w - 20.0F)),
            x + 10.0F, y + 21.0F, UiTheme.text());
        AetherFont.draw(7.5F, sub, x + 10.0F, y + 40.0F, UiTheme.textFaint());
    }

    private void drawShortcut(float x, float y, float w, char glyph, String label, double mx, double my) {
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + 26.0F;
        UiCanvas.gradientRoundRect(x, y, w, 26.0F, 6.0F,
            UiTheme.withAlpha(UiTheme.accent(), hot ? 255 : 215),
            UiTheme.withAlpha(UiTheme.accentDeep(), hot ? 255 : 215));
        AetherFont.drawIcon(glyph, 12.0F, x + 10.0F, y + 7.0F, UiTheme.readableOn(UiTheme.accent()));
        AetherFont.draw(9.0F, label, x + 28.0F, y + (26.0F - AetherFont.height(9.0F)) / 2.0F,
            UiTheme.readableOn(UiTheme.accent()));
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0) {
            return true;
        }
        float x = innerX();
        float y = contentY() + 18.0F - scroll + 26.0F + 56.0F + 14.0F;
        if (my >= y && my < y + 26.0F) {
            if (mx >= x && mx < x + 120.0F) {
                screen.navigateTo(dev.aether.ui.GuiSection.MODULES);
                return true;
            }
            if (mx >= x + 130.0F && mx < x + 250.0F) {
                saveQuietly();
                dev.aether.gui.AetherGui.openHudEditor(client);
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
}
