package dev.aether.hud;

public final class HudElement {
    private final String id;
    private int x;
    private int y;
    private float scale;
    private float opacity;
    private int layer;

    public HudElement(String id, int x, int y) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("HUD element id cannot be blank.");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.scale = 1.0F;
        this.opacity = 1.0F;
        this.layer = 0;
    }

    public String id() {
        return id;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public float scale() {
        return scale;
    }

    public float opacity() {
        return opacity;
    }

    public int layer() {
        return layer;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setScale(float scale) {
        if (scale < 0.25F || scale > 4.0F) {
            throw new IllegalArgumentException("HUD scale must be between 0.25 and 4.0.");
        }
        this.scale = scale;
    }

    public void setOpacity(float opacity) {
        if (opacity < 0.0F || opacity > 1.0F) {
            throw new IllegalArgumentException("HUD opacity must be between 0.0 and 1.0.");
        }
        this.opacity = opacity;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }
}

