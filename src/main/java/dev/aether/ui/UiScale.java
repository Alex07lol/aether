package dev.aether.ui;

import net.minecraft.client.Minecraft;

/**
 * The menu's pixel unit. The visual grammar is specified in "reference pixels" - the
 * numbers the compact client measures out at a 1080p window running GUI scale 2 - and
 * this class reproduces exactly that size at every resolution and every GUI-scale
 * setting: one menu unit is 1/540th of the physical framebuffer height, clamped to a
 * sane range. At 1080p the unit is 2.0 physical pixels, so the 450x280-unit window
 * fills a little under half the screen width - compact at 720p and at 4K alike,
 * regardless of what the vanilla GUI-scale slider does.
 * <p>
 * The menu's mouse coordinates convert through the same unit, so hit-testing and
 * drawing always agree.
 */
public final class UiScale {

    private static float unit = 2.0F;

    private UiScale() {
    }

    /** Recomputes the unit for the current framebuffer. Called once per frame. */
    public static void update() {
        int displayHeight = Minecraft.getMinecraft().displayHeight;
        int displayWidth = Minecraft.getMinecraft().displayWidth;
        if (displayHeight <= 0 || displayWidth <= 0) {
            return;
        }
        float computed = displayHeight / 540.0F;
        unit = Math.max(1.0F, Math.min(3.5F, computed));
    }

    /** Physical pixels per menu unit for the current frame. */
    public static float unit() {
        return unit;
    }

    /** Menu-space width of the framebuffer. */
    public static float menuWidth() {
        return Minecraft.getMinecraft().displayWidth / unit;
    }

    /** Menu-space height of the framebuffer. */
    public static float menuHeight() {
        return Minecraft.getMinecraft().displayHeight / unit;
    }

    /**
     * Converts a mouse coordinate from Minecraft's GUI-scale space into menu space:
     * GUI px -&gt; physical px -&gt; menu units.
     */
    public static float menuFromGui(int guiCoordinate) {
        net.minecraft.client.gui.ScaledResolution resolution =
            new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
        float physical = guiCoordinate * resolution.getScaleFactor();
        return physical / unit;
    }
}
