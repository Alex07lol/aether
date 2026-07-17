package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.settings.GameSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AetherMainMenuScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final AetherClient client;
    private final List<AetherButton> buttons = new ArrayList<AetherButton>();

    public AetherMainMenuScreen(AetherClient client) {
        this.client = client;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    protected void mouseClicked(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void func_73864_a(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            return;
        }
        if (typedChar == 'm' || typedChar == 'M') {
            Mc189Compat.displayGuiScreen(new AetherQuickNavScreen(client, this));
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean func_73868_f() {
        return false;
    }

    private void render(int mouseX, int mouseY) {
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);
        buttons.clear();

        drawBackground(width, height);
        addCornerButtons(width);
        addPrimaryButtons(width, height);

        drawAllButtons(font, mouseX, mouseY);
        drawFooter(font, width, height);
    }

    private void drawBackground(int width, int height) {
        Mc189Compat.drawTexture("background.png", 0, 0, width, height);
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
                Mc189Compat.displayGuiScreen(new AetherModMenuScreen(client, AetherMainMenuScreen.this));
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
        int fill = hover ? AetherUi.withAlpha(AetherUi.COLOR_CARD_HOVER, 145) : AetherUi.withAlpha(AetherUi.COLOR_CARD, 95);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + 1, 0x55FFFFFF);
        String[] parts = button.label().split(":", 3);
        String path = parts.length == 3 ? parts[2] : "icon/info.png";
        int iconSize = Math.max(14, button.width() - 8);
        Mc189Compat.drawTexture(path, button.x() + (button.width() - iconSize) / 2, button.y() + (button.height() - iconSize) / 2, iconSize, iconSize);
    }

    private void drawModernButton(Object font, AetherButton button, int mouseX, int mouseY) {
        boolean hover = button.contains(mouseX, mouseY);
        int fill = hover ? AetherUi.withAlpha(AetherUi.COLOR_CARD_HOVER, 160) : AetherUi.withAlpha(AetherUi.COLOR_CARD, 128);
        int text = AetherUi.COLOR_TEXT_PRIMARY;
        Mc189Compat.drawRect(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
        AetherUi.centered(font, button.label(), button.x(), button.y() + (button.height() - 8) / 2, button.width(), text);
    }

    private void addMenuButton(String label, int x, int y, int width, int height, ScreenAction action) {
        buttons.add(new AetherButton(label, x, y, width, height, action));
    }

    private void addIconButton(String id, String iconPath, int x, int y, int size, ScreenAction action) {
        buttons.add(new AetherButton("__icon__:" + id + ":" + iconPath, x, y, size, size, action));
    }

    private void drawFooter(Object font, int width, int height) {
        AetherUi.text(font, "Copyright Mojang Studios. Do not distribute!", width - Mc189Compat.stringWidth(font, "Copyright Mojang Studios. Do not distribute!") - 4, height - 14, AetherUi.COLOR_TEXT_SECONDARY);
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
