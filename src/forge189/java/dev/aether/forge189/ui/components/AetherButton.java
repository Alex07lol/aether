package dev.aether.forge189.ui.components;

import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

public final class AetherButton {
    private final String label;
    private final int x, y, w, h;

    public AetherButton(String label, int x, int y, int w, int h) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void render(Object font, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        int bg = hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0xEE)
                       : AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xCC);
        int txt = hover ? AetherUi.readableOn(AetherUi.ACCENT)
                        : AetherUi.TEXT_PRIMARY;
        Mc189Compat.drawRect(x, y, x + w, y + h, bg);
        Mc189Compat.drawRect(x, y, x + 2, y + h, AetherUi.ACCENT);
        int txtX = x + (w - Mc189Compat.stringWidth(font, label)) / 2;
        AetherUi.text(font, label, txtX, y + 6, txt);
    }

    public void render(Object font, int mouseX, int mouseY, int x, int y) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0xEE)
                       : AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xCC);
        int txt = hover ? AetherUi.readableOn(AetherUi.ACCENT)
                        : AetherUi.TEXT_PRIMARY;
        Mc189Compat.drawRect(x, y, x + w, y + h, bg);
        Mc189Compat.drawRect(x, y, x + 2, y + h, AetherUi.ACCENT);
        int txtX = x + (w - Mc189Compat.stringWidth(font, label)) / 2;
        AetherUi.text(font, label, txtX, y + 6, txt);
    }

    public void render(Object font, int mouseX, int mouseY, int x, int y, String overrideLabel) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0xEE)
                       : AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xCC);
        int txt = hover ? AetherUi.readableOn(AetherUi.ACCENT)
                        : AetherUi.TEXT_PRIMARY;
        Mc189Compat.drawRect(x, y, x + w, y + h, bg);
        Mc189Compat.drawRect(x, y, x + 2, y + h, AetherUi.ACCENT);
        int txtX = x + (w - Mc189Compat.stringWidth(font, overrideLabel)) / 2;
        AetherUi.text(font, overrideLabel, txtX, y + 6, txt);
    }

    public String label() { return label; }
}