package dev.aether.gui.components;

import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.animation.FrameClock;
import dev.aether.forge189.AetherUi;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;

/**
 * A rounded search field in the Aether language: the palette's search surface, a magnifier glyph,
 * the placeholder while empty, and a caret whose blink is advanced by the frame delta (never by a
 * wall clock read inside the renderer).
 * <p>
 * The screen owns the filtering; this component owns the text and the focus, so the same field can
 * search modules or cosmetics without either screen re-implementing a caret. Typing is delivered by
 * the screen's key path, which is also what lets ESC mean "clear the query" first and "close the
 * screen" only once the field is empty.
 */
public final class SearchBox extends UiComponent {

    private static final int RADIUS = 8;
    private static final int PADDING = 12;
    private static final float CARET_PERIOD = 1060.0F;

    private final String placeholder;
    private final StringBuilder text = new StringBuilder();
    private final Runnable onChange;
    private boolean focused;
    private boolean hovered;
    private float caretMillis;
    private final dev.aether.animation.Anim focus = new dev.aether.animation.Anim(0.0F, 140.0F, Easing.EASE_OUT_QUAD);

    public SearchBox(String placeholder, Runnable onChange) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.onChange = onChange;
        size(0, 40);
    }

    public SearchBox place(int x, int y, int width, int height) {
        at(x, y).size(width, height);
        return this;
    }

    public void update() {
        this.focus.target(this.focused ? 1.0F : 0.0F);
        this.focus.update();
        this.caretMillis += FrameClock.deltaMillis();
        if (this.caretMillis > CARET_PERIOD) {
            this.caretMillis -= CARET_PERIOD;
        }
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int right = left + gw();
        int bottom = top + gh();
        int surface = AetherUi.blend(AetherUi.SEARCH, AetherUi.SEARCH_FOCUS, this.focus.value());
        AetherUi.drawRoundRect(left, top, right, bottom, RADIUS, surface);
        if (this.focus.value() > 0.02F || this.hovered) {
            AetherUi.outline(left, top, right, bottom,
                AnimationMath.scaleAlpha(AetherUi.ACCENT, Math.max(this.focus.value(), this.hovered ? 0.25F : 0.0F) * 0.35F));
        }

        int glyphY = top + (gh() - 11) / 2;
        AetherUi.drawSearchGlyph(left + PADDING, glyphY,
            AetherUi.blend(AetherUi.TEXT_DISABLED, AetherUi.TEXT_SECONDARY, this.focus.value()));

        String shown = this.text.length() == 0 ? this.placeholder : this.text.toString();
        int color = this.text.length() == 0 ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY;
        int textX = left + PADDING + GuiScale.w(20);
        int textY = top + (gh() - AetherFont.height(AetherFont.Size.SMALL)) / 2;
        int available = gw() - (PADDING + GuiScale.w(20)) - PADDING;
        AetherFont.draw(AetherFont.Size.SMALL, AetherFont.trimTo(AetherFont.Size.SMALL, shown, available),
            textX, textY, color);

        if (this.focused && this.text.length() > 0 && this.caretMillis < CARET_PERIOD * 0.55F) {
            int caretX = textX + AetherFont.width(AetherFont.Size.SMALL, this.text.toString());
            AetherFont.draw(AetherFont.Size.SMALL, "_", caretX, textY, AetherUi.ACCENT);
        }
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        this.hovered = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        this.focused = true;
        this.caretMillis = 0.0F;
        return true;
    }

    /** Called by the screen when a click lands elsewhere. */
    public void blur() {
        this.focused = false;
    }

    public boolean focused() {
        return this.focused;
    }

    public String text() {
        return this.text.toString();
    }

    public void clear() {
        this.text.setLength(0);
        notifyChange();
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode) {
        if (!this.focused) {
            return false;
        }
        switch (keyCode) {
            case 14: // backspace
                if (this.text.length() > 0) {
                    this.text.setLength(this.text.length() - 1);
                    notifyChange();
                }
                return true;
            case 211: // delete
                if (this.text.length() > 0) {
                    this.text.setLength(0);
                    notifyChange();
                }
                return true;
            case 1: // ESC clears the query first, and only then leaves the screen
                if (this.text.length() > 0) {
                    clear();
                    return true;
                }
                this.focused = false;
                return false;
            case 28: // enter
                this.focused = false;
                return true;
            default:
                break;
        }
        char character = typedChar;
        if (character >= 32 && character != 127) {
            if (this.text.length() < 64) {
                this.text.append(character);
                notifyChange();
            }
            return true;
        }
        return false;
    }

    private void notifyChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }
}
