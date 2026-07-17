package dev.aether.forge189;

import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public final class AetherInfoScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final String title;
    private final String lineOne;
    private final String lineTwo;
    private final GuiScreen parent;

    public AetherInfoScreen(String title, String lineOne, String lineTwo, GuiScreen parent) {
        this.title = title;
        this.lineOne = lineOne;
        this.lineTwo = lineTwo;
        this.parent = parent;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
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
        AetherUi.background(width, height, System.currentTimeMillis());
        int left = width / 2 - 150;
        int top = height / 2 - 44;
        AetherUi.panel(left, top, left + 300, top + 88);
        AetherUi.centered(font, title, left, top + 14, 300, AetherUi.ACCENT_DARK);
        AetherUi.centered(font, lineOne, left, top + 36, 300, AetherUi.TEXT_DARK);
        AetherUi.centered(font, lineTwo, left, top + 52, 300, AetherUi.TEXT_MUTED);
        AetherUi.centered(font, "Esc to return", left, top + 70, 300, AetherUi.TEXT_MUTED);
    }
}

