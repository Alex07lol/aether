package dev.aether.platform;

import dev.aether.event.Event;

public final class HudRenderEvent implements Event {
    private final int scaledWidth;
    private final int scaledHeight;
    private final float partialTicks;

    public HudRenderEvent(int scaledWidth, int scaledHeight, float partialTicks) {
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
        this.partialTicks = partialTicks;
    }

    public int scaledWidth() {
        return scaledWidth;
    }

    public int scaledHeight() {
        return scaledHeight;
    }

    public float partialTicks() {
        return partialTicks;
    }
}

