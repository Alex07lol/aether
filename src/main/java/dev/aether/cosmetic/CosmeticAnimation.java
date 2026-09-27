package dev.aether.cosmetic;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds pre-loaded frames for an animated cosmetic (e.g. animated cape).
 * The frames are loaded once on first use and cached.
 */
public final class CosmeticAnimation {

    private final List<BufferedImage> frames;
    private final int frameRate; // frames per second
    private long startTime;

    public CosmeticAnimation(List<BufferedImage> frames, int frameRate) {
        this.frames = new ArrayList<>(frames);
        this.frameRate = Math.max(1, frameRate);
        this.startTime = System.currentTimeMillis();
    }

    /** Return the current frame based on the elapsed time. */
    public BufferedImage currentFrame() {
        if (frames.isEmpty()) return null;
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        int idx = (int) ((elapsed / 1000f) * frameRate) % frames.size();
        return frames.get(idx);
    }

    /** Reset the animation (e.g. when the player equips a new animated cape). */
    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public int frameCount() { return frames.size(); }
    public int frameRate() { return frameRate; }
}