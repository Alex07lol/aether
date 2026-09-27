package dev.aether.forge189.ui;

public final class AetherToggle {
    private final int x, y, w, h;
    private final boolean on;

    public AetherToggle(int x, int y, int w, int h, boolean on) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.on = on;
    }

    public void render(boolean hover) {
        // Simplified toggle rendering
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}