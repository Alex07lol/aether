package net.minecraft.client.renderer;

/**
 * Compile- and test-time stand-in for Minecraft's GlStateManager.
 * <p>
 * Every state call is a no-op except {@code color}, which records the last colour it was given. That
 * one piece of observable state is what lets the headless font test prove the property the UI now
 * depends on: text takes its colour from the call, not from whatever colour state was left behind,
 * and a finished text draw leaves that state at white.
 */
public class GlStateManager {
    private static float lastRed = 1.0F;
    private static float lastGreen = 1.0F;
    private static float lastBlue = 1.0F;
    private static float lastAlpha = 1.0F;

    public static void pushMatrix() {}
    public static void popMatrix() {}
    public static void scale(float x, float y, float z) {}
    public static void enableTexture2D() {}
    public static void disableTexture2D() {}
    public static void enableBlend() {}
    public static void disableBlend() {}
    public static void blendFunc(int srcFactor, int dstFactor) {}
    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {}
    public static void depthMask(boolean flag) {}
    public static void enableRescaleNormal() {}
    public static void disableRescaleNormal() {}

    public static void color(float red, float green, float blue, float alpha) {
        lastRed = red;
        lastGreen = green;
        lastBlue = blue;
        lastAlpha = alpha;
    }

    /** @return {red, green, blue, alpha} as last set, or white when colour was never set. */
    public static float[] lastColor() {
        return new float[] {lastRed, lastGreen, lastBlue, lastAlpha};
    }

    /** Puts the recorded colour back to white, so one test cannot inherit another's state. */
    public static void resetLastColor() {
        color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
