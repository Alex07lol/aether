package dev.aether.forge189;

import dev.aether.AetherClient;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Quick Navigation Overlay Screen triggered by Right-Shift.
 * Displays a clean, background-less overlay in the screen center with 3 buttons:
 *  - Left: Cosmetics Management
 *  - Center: Mod Menu ("Mod menu")
 *  - Right: HUD Editor
 */
public final class AetherQuickNavScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final AetherClient client;
    private final GuiScreen parent;

    public AetherQuickNavScreen(AetherClient client) {
        this(client, null);
    }

    public AetherQuickNavScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // NO background fill or default background - completely transparent!

        Object font = Mc189Compat.screenFontRenderer(this);

        int btnWidth = 114;
        int btnHeight = 36;
        int gap = 14;
        int totalWidth = (btnWidth * 3) + (gap * 2);

        int startX = (width - totalWidth) / 2;
        int startY = (height - btnHeight) / 2;

        // Button 1: Cosmetics Management (Left)
        int leftX = startX;
        boolean hoverLeft = mouseX >= leftX && mouseX <= leftX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight;
        drawNavButton(font, "Cosmetics", leftX, startY, btnWidth, btnHeight, hoverLeft, 0xFF718096, false);

        // Button 2: Mod Menu (Center)
        int centerX = leftX + btnWidth + gap;
        boolean hoverCenter = mouseX >= centerX && mouseX <= centerX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight;
        drawNavButton(font, "Mod menu", centerX, startY, btnWidth, btnHeight, hoverCenter, 0xFF52BEEB, true);

        // Button 3: HUD Editor (Right)
        int rightX = centerX + btnWidth + gap;
        boolean hoverRight = mouseX >= rightX && mouseX <= rightX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight;
        drawNavButton(font, "HUD Editor", rightX, startY, btnWidth, btnHeight, hoverRight, 0xFF718096, false);
    }

    private void drawNavButton(Object font, String title, int x, int y, int w, int h, boolean hover, int accentColor, boolean isPrimary) {
        // Transparent dark glass card background
        int bgColor = hover ? 0xF01A202C : (isPrimary ? 0xDD1A202C : 0xC010141D);
        int borderColor = hover ? accentColor : (isPrimary ? 0x8852BEEB : 0x33FFFFFF);

        Mc189Compat.drawRect(x, y, x + w, y + h, bgColor);

        // Border outline
        Mc189Compat.drawRect(x, y, x + w, y + 1, borderColor);
        Mc189Compat.drawRect(x, y + h - 1, x + w, y + h, borderColor);
        Mc189Compat.drawRect(x, y, x + 1, y + h, borderColor);
        Mc189Compat.drawRect(x + w - 1, x + w, y, y + h, borderColor);

        // Accent top bar on hover or primary
        if (hover || isPrimary) {
            Mc189Compat.drawRect(x, y, x + w, y + 2, accentColor);
        }

        // Title text centered
        int textW = Mc189Compat.stringWidth(font, title);
        int textX = x + (w - textW) / 2;
        int textY = y + (h - 8) / 2;
        int textColor = hover ? 0xFFFFFFFF : (isPrimary ? 0xFF52BEEB : 0xFFCBD5E0);
        Mc189Compat.drawStringWithShadow(font, title, textX, textY, textColor);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        int btnWidth = 114;
        int btnHeight = 36;
        int gap = 14;
        int totalWidth = (btnWidth * 3) + (gap * 2);

        int startX = (width - totalWidth) / 2;
        int startY = (height - btnHeight) / 2;

        int leftX = startX;
        int centerX = leftX + btnWidth + gap;
        int rightX = centerX + btnWidth + gap;

        // Left: Cosmetics Management
        if (mouseX >= leftX && mouseX <= leftX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight) {
            Mc189Compat.displayGuiScreen(new AetherCosmeticsScreen(client, this));
            return;
        }

        // Center: Mod menu
        if (mouseX >= centerX && mouseX <= centerX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight) {
            Mc189Compat.displayGuiScreen(new AetherModMenuScreen(client, this));
            return;
        }

        // Right: HUD Editor
        if (mouseX >= rightX && mouseX <= rightX + btnWidth && mouseY >= startY && mouseY <= startY + btnHeight) {
            Mc189Compat.displayGuiScreen(new AetherHudEditorScreen(client));
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
