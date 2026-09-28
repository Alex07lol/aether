package net.minecraft.client.renderer;

import java.util.Arrays;

import net.minecraft.client.renderer.vertex.VertexFormat;

/**
 * Compile- and test-time stand-in for Minecraft's WorldRenderer.
 * <p>
 * Its signatures are Minecraft's, so the adapter links against the real class at runtime, but the
 * body records what was emitted instead of buffering into a GL upload. That is what lets the
 * headless self-tests assert the one thing a screenshot-free environment otherwise cannot: that a
 * glyph quad really was produced, with the expected positions, texture coordinates and colour, using
 * the expected vertex layout.
 */
public class WorldRenderer {
    /** Cap on the vertices one recorded batch keeps. */
    private static final int MAX_VERTICES = 4096;

    /**
     * Snapshots are opt-in: a game-like run draws hundreds of primitives per frame, and copying
     * every batch would be pure overhead for a test that only cares that something was drawn.
     */
    private static boolean recording;

    private static Batch lastBatch;

    private int mode;
    private VertexFormat format;
    private boolean open;
    private int count;

    private final float[] positions = new float[MAX_VERTICES * 3];
    private final float[] texCoords = new float[MAX_VERTICES * 2];
    private final float[] colors = new float[MAX_VERTICES * 4];
    private final boolean[] hasTexCoords = new boolean[MAX_VERTICES];
    private final boolean[] hasColor = new boolean[MAX_VERTICES];

    private float pendingX;
    private float pendingY;
    private float pendingZ;
    private float pendingU;
    private float pendingV;
    private float pendingRed;
    private float pendingGreen;
    private float pendingBlue;
    private float pendingAlpha;
    private boolean vertexHasTexCoords;
    private boolean vertexHasColor;

    public void begin(int mode, VertexFormat format) {
        this.mode = mode;
        this.format = format;
        this.open = true;
        this.count = 0;
        this.vertexHasTexCoords = false;
        this.vertexHasColor = false;
    }

    public WorldRenderer pos(double x, double y, double z) {
        this.pendingX = (float) x;
        this.pendingY = (float) y;
        this.pendingZ = (float) z;
        return this;
    }

    public WorldRenderer tex(double u, double v) {
        this.pendingU = (float) u;
        this.pendingV = (float) v;
        this.vertexHasTexCoords = true;
        return this;
    }

    public WorldRenderer color(float red, float green, float blue, float alpha) {
        this.pendingRed = red;
        this.pendingGreen = green;
        this.pendingBlue = blue;
        this.pendingAlpha = alpha;
        this.vertexHasColor = true;
        return this;
    }

    public void endVertex() {
        if (!open || count >= MAX_VERTICES) {
            return;
        }
        positions[count * 3] = pendingX;
        positions[count * 3 + 1] = pendingY;
        positions[count * 3 + 2] = pendingZ;
        texCoords[count * 2] = pendingU;
        texCoords[count * 2 + 1] = pendingV;
        colors[count * 4] = pendingRed;
        colors[count * 4 + 1] = pendingGreen;
        colors[count * 4 + 2] = pendingBlue;
        colors[count * 4 + 3] = pendingAlpha;
        hasTexCoords[count] = vertexHasTexCoords;
        hasColor[count] = vertexHasColor;
        count++;
    }

    /** Called by the stub Tessellator's {@code draw()}. */
    void publishBatch() {
        if (!open) {
            return;
        }
        if (!recording) {
            open = false;
            return;
        }
        lastBatch = new Batch(mode, format, count,
            Arrays.copyOf(positions, count * 3),
            Arrays.copyOf(texCoords, count * 2),
            Arrays.copyOf(colors, count * 4),
            Arrays.copyOf(hasTexCoords, count),
            Arrays.copyOf(hasColor, count));
        open = false;
    }

    /** @return true when {@code draw()} keeps a snapshot of the batch for the tests to inspect. */
    public static boolean isRecording() {
        return recording;
    }

    /** Enables or disables per-draw snapshots; see {@link #isRecording()}. */
    public static void setRecording(boolean enabled) {
        recording = enabled;
    }

    /** @return the batch the last {@code draw()} published, or null when nothing was recorded. */
    public static Batch lastBatch() {
        return lastBatch;
    }

    public static void resetRecorder() {
        lastBatch = null;
    }

    /** An immutable snapshot of one begin/draw pair. */
    public static final class Batch {
        /** The GL primitive mode passed to {@code begin} (7 is quads). */
        public final int mode;
        /** The vertex layout passed to {@code begin}; compare against DefaultVertexFormats. */
        public final VertexFormat format;
        /** How many vertices were emitted. */
        public final int vertexCount;

        private final float[] positions;
        private final float[] texCoords;
        private final float[] colors;
        private final boolean[] hasTexCoords;
        private final boolean[] hasColor;

        Batch(int mode, VertexFormat format, int vertexCount, float[] positions, float[] texCoords,
              float[] colors, boolean[] hasTexCoords, boolean[] hasColor) {
            this.mode = mode;
            this.format = format;
            this.vertexCount = vertexCount;
            this.positions = positions;
            this.texCoords = texCoords;
            this.colors = colors;
            this.hasTexCoords = hasTexCoords;
            this.hasColor = hasColor;
        }

        public float x(int vertex) {
            return positions[vertex * 3];
        }

        public float y(int vertex) {
            return positions[vertex * 3 + 1];
        }

        public float z(int vertex) {
            return positions[vertex * 3 + 2];
        }

        public float u(int vertex) {
            return texCoords[vertex * 2];
        }

        public float v(int vertex) {
            return texCoords[vertex * 2 + 1];
        }

        public float red(int vertex) {
            return colors[vertex * 4];
        }

        public float green(int vertex) {
            return colors[vertex * 4 + 1];
        }

        public float blue(int vertex) {
            return colors[vertex * 4 + 2];
        }

        public float alpha(int vertex) {
            return colors[vertex * 4 + 3];
        }

        public boolean hasTexCoords(int vertex) {
            return hasTexCoords[vertex];
        }

        public boolean hasColor(int vertex) {
            return hasColor[vertex];
        }
    }
}
