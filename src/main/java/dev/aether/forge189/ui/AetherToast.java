package dev.aether.forge189.ui;

import dev.aether.forge189.ui.AetherMetrics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AetherToast {
    private final String text;
    private final int accent;
    private final long createdAt;
    private final long expireAt;
    private float enterProgress;
    private float exitProgress;

    public AetherToast(String text, int accent) {
        this.text = text;
        this.accent = accent;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expireAt = now + AetherMetrics.TOAST_DURATION;
        this.enterProgress = 0f;
        this.exitProgress = 0f;
    }

    public String text() { return text; }
    public int accent() { return accent; }
    public long expireAt() { return expireAt; }

    public void tick(long now) {
        if (now < createdAt + AetherMetrics.TOAST_ENTER) {
            enterProgress = Math.min(1f, (now - createdAt) / (float) AetherMetrics.TOAST_ENTER);
        } else if (now > expireAt - AetherMetrics.TOAST_EXIT) {
            exitProgress = Math.min(1f, (now - (expireAt - AetherMetrics.TOAST_EXIT)) / (float) AetherMetrics.TOAST_EXIT);
        }
    }

    public boolean alive(long now) {
        return now < expireAt || exitProgress < 1f;
    }

    public float visibility() {
        if (enterProgress < 1f) return enterProgress;
        if (exitProgress > 0f) return 1f - exitProgress;
        return 1f;
    }
}