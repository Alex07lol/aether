package org.lwjgl.opengl;

import java.nio.IntBuffer;

/**
 * Compile-time shim for LWJGL's {@code org.lwjgl.opengl.GL11}. Only the entry points Aether
 * references are declared - directly, or by name through {@code Mc189Compat}'s reflective fallbacks -
 * and every one of them exists in LWJGL 2.9 (which is what Minecraft 1.8.9 ships), so the adapter
 * links against the real library at runtime.
 * <p>
 * Immediate-mode and client-vertex-array entry points are deliberately absent: the UI draws through
 * the Tessellator vertex pipeline, so no Aether screen can depend on client-array state or on
 * {@code glBegin}/{@code glEnd}.
 */
public class GL11 {
    public static final int GL_QUADS = 7;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_RGBA = 6408;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_LINEAR = 9729;

    /* Caps passed to the reflective glEnable/glDisable fallbacks, by their OpenGL numbers. */
    public static final int GL_BLEND = 3042;
    public static final int GL_ALPHA_TEST = 3008;
    public static final int GL_DEPTH_TEST = 2929;

    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glScaled(double x, double y, double z) {}
    public static void glTexParameteri(int target, int pname, int param) {}
    public static void glBindTexture(int target, int texture) {}

    /**
     * Hands out a fresh non-zero texture name, as OpenGL does. The headless tests rely on this:
     * without it the glyph atlas could never be uploaded, so the draw path they are meant to cover
     * would never run.
     */
    public static int glGenTextures() {
        return nextTextureName++;
    }

    public static void glDeleteTextures(int texture) {}

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height,
                                    int border, int format, int type, IntBuffer pixels) {}

    public static int glGetInteger(int pname) {
        return 0;
    }

    private static int nextTextureName = 1;

    // Present so the screenshot path resolves; no-ops without a real GL context.
    public static void glReadPixels(int x, int y, int width, int height, int format, int type, java.nio.ByteBuffer buffer) {}
}
