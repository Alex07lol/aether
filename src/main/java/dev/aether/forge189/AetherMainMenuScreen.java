package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.forge189.font.AetherFontManager;
import dev.aether.forge189.font.GlyphPageFontRenderer;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.settings.GameSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AetherMainMenuScreen extends GuiScreen implements dev.aether.gui.core.AetherUiScreen {
    private static final int KEY_ESCAPE = 1;
    private static final AetherFontManager fontManager = AetherFontManager.instance();

    private final AetherClient client;
    private final List<AetherButton> buttons = new ArrayList<AetherButton>();

    public AetherMainMenuScreen(AetherClient client) {
        this.client = client;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }


    protected void mouseClicked(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }


    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            return;
        }
        if (typedChar == 'm' || typedChar == 'M') {
            dev.aether.gui.AetherGui.open(client, dev.aether.ui.GuiSection.MODULES);
        }
    }


    public boolean doesGuiPauseGame() {
        return false;
    }


    private void render(int mouseX, int mouseY) {
        AetherUi.syncTheme();
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);
        Object smoothFont = fontManager.uiFont();
        if (smoothFont != null) {
            font = smoothFont;
        }
        buttons.clear();

        drawBackground(width, height);
        addCornerButtons(width);
        addPrimaryButtons(width, height);

        // AETHER branding
        if (font instanceof GlyphPageFontRenderer) {
            ((GlyphPageFontRenderer) font).drawString("AETHER",
                width / 2 - ((GlyphPageFontRenderer) font).getStringWidth("AETHER") / 2,
                height / 2 - 80,
                AetherUi.ACCENT);
        } else {
            AetherUi.centered(font, "AETHER", 0, height / 2 - 80, width, AetherUi.ACCENT);
        }

        drawAllButtons(font, mouseX, mouseY);
        drawFooter(font, width, height);
    }

    private void drawBackground(int width, int height) {
        // The custom Aether background art, stretched to fill, with a light dark scrim on top
        // so text stays readable without hiding the artwork.
        Mc189Compat.drawTexture("background.png", 0, 0, width, height);
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            int top = i * height / steps;
            int bottom = (i + 1) * height / steps + 1;
            float t = (float) i / steps;
            int alpha = (int) (0x5E + (0x3C - 0x5E) * t);
            int base = AetherUi.lerpColor(0x0A0C12, 0x05070A, t);
            Mc189Compat.drawRect(0, top, width, bottom, (alpha << 24) | (base & 0x00FFFFFF));
        }
    }

    private void addPrimaryButtons(int width, int height) {
        int buttonWidth = clamp(width / 6, 110, 140);
        int buttonHeight = clamp(height / 30, 18, 24);
        int left = width / 2 - buttonWidth / 2;
        int top = Math.max(108, height / 2 + 8);
        int gap = buttonHeight + 6;

        addMenuButton("Singleplayer", left, top, buttonWidth, buttonHeight, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(new GuiSelectWorld(AetherMainMenuScreen.this));
            }
        });
        addMenuButton("Multiplayer", left, top + gap, buttonWidth, buttonHeight, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(new GuiMultiplayer(AetherMainMenuScreen.this));
            }
        });
    }

    private void addCornerButtons(int width) {
        int size = clamp(width / 28, 22, 28);
        int gap = 7;
        int top = 14;
        int left = 14;
        int right = width - 14 - size;

        addIconButton("mod_menu", "icon/main_mod_menu.png", left, top, size, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(dev.aether.gui.AetherGui.modules(client));
            }
        });
        addIconButton("resource_packs", "icon/main_resource_pack.png", left + size + gap, top, size, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(new GuiScreenResourcePacks(AetherMainMenuScreen.this));
            }
        });
        addIconButton("settings", "icon/main_settings.png", right - size - gap, top, size, new ScreenAction() {
            public void run() {
                openSettings();
            }
        });
        addIconButton("quit", "icon/main_quit.png", right, top, size, new ScreenAction() {
            public void run() {
                Mc189Compat.shutdown();
            }
        });
    }

    private void addFooterButtons(int width, int height) {
        // The user requested to remove the footer buttons.
    }

    private void drawAllButtons(Object font, int mouseX, int mouseY) {
        for (AetherButton button : buttons) {
            if (button.label().startsWith("__icon__:")) {
                drawIconButton(font, button, mouseX, mouseY);
            } else {
                drawModernButton(font, button, mouseX, mouseY);
            }
        }
    }

    private void drawIconButton(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int r = 8;
        // Shadow
        AetherUi.drawRoundRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), r,
            AetherUi.withAlpha(AetherUi.SHADOW, 0x33));
        // Button body
        int fill = hover ? AetherUi.withAlpha(AetherUi.CARD_HOVER, 0xCC) : AetherUi.withAlpha(AetherUi.CARD, 0xAA);
        AetherUi.drawRoundRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), r, fill);
        // Top accent
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + 2,
            hover ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.ACCENT, 0x88));
        // Border
        AetherUi.outline(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(),
            hover ? AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x18));
        // Icon
        String[] parts = button.label().split(":", 3);
        String path = parts.length == 3 ? parts[2] : "icon/info.png";
        int iconSize = Math.max(14, button.width() - 8);
        Mc189Compat.drawTexture(path, button.x() + (button.width() - iconSize) / 2, button.y() + (button.height() - iconSize) / 2, iconSize, iconSize);
    }

    private void drawModernButton(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int r = 8; // corner radius
        // Shadow
        AetherUi.drawRoundRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), r,
            AetherUi.withAlpha(AetherUi.SHADOW, 0x33));
        // Button body - glass effect
        int fill = hover ? AetherUi.withAlpha(AetherUi.CARD_HOVER, 0xDD) : AetherUi.withAlpha(AetherUi.CARD, 0xBB);
        AetherUi.drawRoundRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), r, fill);
        // Left accent bar
        int accentColor = hover ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.ACCENT, 0x66);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + 2, button.y() + button.height(), accentColor);
        // Border
        AetherUi.outline(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(),
            hover ? AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x33) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x18));
        // Text - use smooth font if available
        if (font instanceof GlyphPageFontRenderer) {
            ((GlyphPageFontRenderer) font).drawString(button.label(),
                button.x() + (button.width() - ((GlyphPageFontRenderer) font).getStringWidth(button.label())) / 2,
                button.y() + (button.height() - 8) / 2,
                hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        } else {
            AetherUi.centered(font, button.label(), button.x(), button.y() + (button.height() - 8) / 2, button.width(),
                hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        }
    }

    private void addMenuButton(String label, int x, int y, int width, int height, ScreenAction action) {
        buttons.add(new AetherButton(label, x, y, width, height, action));
    }

    private void addIconButton(String id, String iconPath, int x, int y, int size, ScreenAction action) {
        buttons.add(new AetherButton("__icon__:" + id + ":" + iconPath, x, y, size, size, action));
    }

    private void drawFooter(Object font, int width, int height) {
        String copyright = "Copyright Mojang Studios. Do not distribute!";
        int textX = width - Mc189Compat.stringWidth(font, copyright) - 8;
        int textY = height - 24;
        AetherUi.text(font, copyright, textX, textY, AetherUi.TEXT_DISABLED);
    }

    private void click(int mouseX, int mouseY, int clickedButton) {
        if (clickedButton != 0) {
            return;
        }
        for (AetherButton button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                button.click();
                return;
            }
        }
    }

    private void openSettings() {
        Mc189Compat.displayGuiScreen(new GuiOptions(this, (GameSettings) Mc189Compat.gameSettings(Mc189Compat.minecraft())));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
