package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherButton {
    private final String label;
    private final int x, y, w, h;
    private final int cornerRadius;

    public AetherButton(String label, int x, int y, int w, int h) {
        this(label, x, y, w, h, 4);
    }

    public AetherButton(String label, int x, int y, int w, int h, int cornerRadius) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.cornerRadius = cornerRadius;
    }

    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /** Renders the button at its stored position with the given mouse coordinates. */
    public void render(Object font, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        draw(font, x, y, hover);
    }

    /** Renders the button at an overridden position (e.g. page-specific layout). */
    public void render(Object font, int mouseX, int mouseY, int ox, int oy) {
        boolean hover = mouseX >= ox && mouseX <= ox + w && mouseY >= oy && mouseY <= oy + h;
        draw(font, ox, oy, hover);
    }

    public void render(Object font, int mouseX, int mouseY, int ox, int oy, String overrideLabel) {
        boolean hover = mouseX >= ox && mouseX <= ox + w && mouseY >= oy && mouseY <= oy + h;
        draw(font, ox, oy, hover, overrideLabel);
    }

    public String label() { return label; }

    private void draw(Object font, int ox, int oy, boolean hover) {
        draw(font, ox, oy, hover, label);
    }

    private void draw(Object font, int ox, int oy, boolean hover, String txt) {
        int bg = hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0xCC)
                       : AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xBB);
        int border = hover ? AetherUi.ACCENT_ON : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x44);
        int text = hover ? AetherUi.readableOn(AetherUi.ACCENT)
                         : AetherUi.TEXT_PRIMARY;

        int r = Math.min(cornerRadius, Math.min(w / 2, h / 2));
        AetherUi.drawRoundRect(ox, oy, ox + w, oy + h, r, bg);
        AetherUi.outline(ox, oy, ox + w, oy + h, border);
        int txtX = ox + (w - Mc189Compat.stringWidth(font, txt)) / 2;
        AetherUi.text(font, txt, txtX, oy + (h > 20 ? 6 : 3), text);
    }
}