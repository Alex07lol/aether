package net.minecraft.client.renderer;

/**
 * Compile- and test-time stand-in for Minecraft's Tessellator.
 * <p>
 * It hands out one shared {@link WorldRenderer} and counts {@code draw()} calls, so a headless test
 * can tell a batched draw path from one draw per primitive - the property that made moving the
 * glyph quads onto the vertex pipeline worth doing.
 */
public class Tessellator {
    private static final Tessellator INSTANCE = new Tessellator();

    private final WorldRenderer worldRenderer = new WorldRenderer();
    private static int recordedDraws;

    public static Tessellator getInstance() {
        return INSTANCE;
    }

    public WorldRenderer getWorldRenderer() {
        return worldRenderer;
    }

    public void draw() {
        recordedDraws++;
        worldRenderer.publishBatch();
    }

    /** @return how many {@code draw()} calls happened since the recorder was last reset. */
    public static int recordedDraws() {
        return recordedDraws;
    }

    /**
     * Turns per-draw vertex snapshots on or off. The draw counter always works; the snapshots are
     * opt-in because a frame that draws hundreds of primitives would otherwise copy every batch.
     */
    public static void setRecording(boolean enabled) {
        WorldRenderer.setRecording(enabled);
    }

    /** Clears the draw counter and the last recorded batch. */
    public static void resetRecorder() {
        recordedDraws = 0;
        WorldRenderer.resetRecorder();
    }
}
