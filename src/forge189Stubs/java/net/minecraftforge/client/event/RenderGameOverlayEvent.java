package net.minecraftforge.client.event;

import net.minecraft.client.gui.ScaledResolution;

public class RenderGameOverlayEvent {
    public ElementType type;
    public ScaledResolution resolution;
    public float partialTicks;

    public enum ElementType { ALL, BOSSHEALTH, CROSSHAIRS, TEXT }
    public static class Pre extends RenderGameOverlayEvent {
        public void setCanceled(boolean canceled) {}
    }
    public static class Post extends RenderGameOverlayEvent {}
}
