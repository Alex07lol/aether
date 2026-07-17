package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import java.io.IOException;

public class GuiScreen extends Gui {
    public int width;
    public int height;
    public FontRenderer fontRendererObj;
    public Minecraft mc;

    public void initGui() {}
    public void onGuiClosed() {}
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {}
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {}
    protected void mouseReleased(int mouseX, int mouseY, int state) {}
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {}
    public void handleMouseInput() throws IOException {}
    protected void keyTyped(char typedChar, int keyCode) throws IOException {}
    public boolean doesGuiPauseGame() { return false; }
    public static boolean isShiftKeyDown() { return false; }
}
