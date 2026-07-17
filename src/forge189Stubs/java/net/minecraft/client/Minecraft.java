package net.minecraft.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.world.World;

public class Minecraft {
    public FontRenderer fontRendererObj;
    public GuiScreen currentScreen;
    public int displayWidth;
    public int displayHeight;
    public World theWorld;
    public EntityPlayerSP thePlayer;

    public static Minecraft getMinecraft() { return null; }
    public static int getDebugFPS() { return 0; }
    public void displayGuiScreen(GuiScreen guiScreenIn) {}
    public void shutdown() {}
    public RenderItem getRenderItem() { return null; }
}
