package org.lwjgl.opengl;

public class GL11 {
    public static final int GL_TRIANGLES = 4;
    public static final int GL_QUADS = 7;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_LINEAR = 9729;
    public static final int GL_BLEND = 3042;
    public static final int GL_ALPHA_TEST = 3008;
    public static final int GL_DEPTH_TEST = 2929;

    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glScaled(double x, double y, double z) {}
    public static void glBegin(int mode) {}
    public static void glEnd() {}
    public static void glTexCoord2f(float s, float t) {}
    public static void glVertex2f(float x, float y) {}
    public static void glTexParameteri(int target, int pname, int param) {}

    // Present so the screenshot path resolves; no-ops without a real GL context.
    public static void glReadPixels(int x, int y, int width, int height, int format, int type, java.nio.ByteBuffer buffer) {}
}
