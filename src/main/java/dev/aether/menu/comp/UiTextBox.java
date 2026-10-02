package dev.aether.menu.comp;

import dev.aether.gui.AetherFont;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The single-line text field of the grammar: a rounded card-coloured box with the
 * typed text, a blinking caret, horizontal clipping when the text overflows, and the
 * focused state shown by a brighter fill. The field commits through a callback on
 * Enter and on defocus, exactly when the reference commits.
 */
public final class UiTextBox {

    private static final int KEY_BACKSPACE = 14;
    private static final int KEY_RETURN = 28;

    private final float width;
    private final float height;
    private final String placeholder;
    private final Runnable onCommit;
    private final StringBuilder text = new StringBuilder();
    private boolean focused;
    private int scrollPx;

    public UiTextBox(float width, float height, String placeholder, Runnable onCommit) {
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        this.onCommit = onCommit;
    }

    public String text() {
        return text.toString();
    }

    /** Clears the field and drops focus (used when a caller resets it wholesale). */
    public void reset() {
        text.setLength(0);
        focused = false;
        scrollPx = 0;
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            text.append(value);
        }
        scrollPx = 0;
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void draw(float x, float y, double mx, double my) {
        boolean hot = mx >= x && mx < x + width && my >= y && my < y + height;
        UiCanvas.roundRect(x, y, width, height, 4.0F,
            focused ? UiTheme.withAlpha(UiTheme.cardHover(), 255)
                : UiTheme.withAlpha(UiTheme.card(), hot ? 255 : 230));
        UiCanvas.outline(x, y, width, height, 4.0F,
            focused ? UiTheme.withAlpha(UiTheme.accent(), 150) : UiTheme.edge(), 1.0F);

        float pad = 5.0F;
        float textY = y + (height - AetherFont.height(8.0F)) / 2.0F;
        float maxTextW = width - pad * 2.0F - 1.0F;
        String shown = text.toString();
        int textW = AetherFont.width(8.0F, shown);
        // Keep the caret end visible: scroll when the text is wider than the field.
        scrollPx = Math.max(0, Math.min(textW - (int) maxTextW, Math.max(scrollPx, 0)));
        UiCanvas.scissor(x + 1.0F, y, width - 2.0F, height);
        if (shown.isEmpty() && !focused) {
            AetherFont.draw(8.0F, placeholder, x + pad, textY, UiTheme.withAlpha(UiTheme.textSoft(), 200));
        } else {
            AetherFont.draw(8.0F, shown, x + pad - scrollPx, textY,
                shown.isEmpty() ? UiTheme.textFaint() : UiTheme.text());
            if (focused) {
                boolean blink = (System.currentTimeMillis() / 600L) % 2L == 0L;
                if (blink) {
                    int caretX = Math.round(x + pad + textW - scrollPx);
                    UiCanvas.roundRect(caretX, textY, 1.0F, AetherFont.height(8.0F), 0.0F, UiTheme.text());
                }
            }
        }
        UiCanvas.clearScissor();
    }

    public boolean click(float x, float y, double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            focused = true;
            return true;
        }
        return false;
    }

    /** Defocus-and-commit, called when a click lands elsewhere. */
    public void clickOutside() {
        if (focused) {
            focused = false;
            commit();
        }
    }

    public boolean key(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == KEY_BACKSPACE) {
            if (text.length() > 0) {
                text.deleteCharAt(text.length() - 1);
            }
            return true;
        }
        if (keyCode == KEY_RETURN) {
            focused = false;
            commit();
            return true;
        }
        if (typedChar >= 32 && typedChar < 127 && text.length() < 48) {
            text.append(typedChar);
            return true;
        }
        return keyCode != 0;
    }

    private void commit() {
        if (onCommit != null) {
            onCommit.run();
        }
    }
}
