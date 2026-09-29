package dev.aether.gui.components;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * The general-purpose button of the Aether GUI - the equivalent of Leaf Client's
 * {@code SystemButton}/{@code ModButton} click targets, drawn procedurally in Aether's
 * dark-glass identity instead of from textures. See docs/GUI_REBUILD.md.
 */
public class Button extends UiComponent {

    /** Visual weight of the button. */
    public enum Style {
        /** Quiet glass surface for rows and secondary actions. */
        QUIET,
        /** Accent-tinted surface for the primary action of a view. */
        PRIMARY,
        /** Warning-tinted surface for destructive actions. */
        DANGER
    }

    private String label;
    private final Style style;
    private final Runnable action;
    private boolean hover;
    private boolean pressed;

    public Button(String label, Style style, Runnable action) {
        this.label = label;
        this.style = style;
        this.action = action;
    }

    public Button at(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        int radius = GuiScale.h(6);
        int fill;
        int textColor;
        switch (style) {
            case PRIMARY:
                fill = hover ? AetherUi.ACCENT : AetherUi.withAlpha(AetherUi.ACCENT, 0xCC);
                textColor = AetherUi.readableOn(AetherUi.ACCENT);
                break;
            case DANGER:
                fill = hover ? AetherUi.WARN : AetherUi.withAlpha(AetherUi.WARN, 0xB3);
                textColor = AetherUi.readableOn(AetherUi.WARN);
                break;
            default:
                fill = hover ? AetherUi.ROW_HOVER : AetherUi.ROW_BG;
                textColor = hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY;
                break;
        }
        AetherUi.drawRoundRect(left, top, left + w, top + h, radius, fill);
        AetherUi.outline(left, top, left + w, top + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, hovered() ? 0x50 : 0x28));
        if (pressed && hover) {
            Mc189Compat.drawRect(left, top, left + w, top + h, AetherUi.withAlpha(0xFF000000, 0x33));
        }
        AetherFont.drawCentered(AetherFont.Size.BODY, label, left, top + (h - AetherFont.height(AetherFont.Size.BODY)) / 2, w, textColor);
    }

    protected boolean hovered() {
        return hover;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        pressed = true;
        if (action != null) {
            action.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        pressed = false;
        hover = contains(mouseX, mouseY);
    }
}
