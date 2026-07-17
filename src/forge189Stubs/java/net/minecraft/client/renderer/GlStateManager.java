package net.minecraft.client.renderer;

public class GlStateManager {
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
    public static void color(float red, float green, float blue, float alpha) {}
    public static void enableRescaleNormal() {}
    public static void disableRescaleNormal() {}
}
