package dev.aether.menu.comp;

import dev.aether.gui.AetherFont;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The header search field of the grammar: 160x18, radius 6, card-coloured, with the
 * magnifier glyph at the left, the typed query, a placeholder that slides and fades
 * when focused, and a blinking caret. Typing any printable key while its category is
 * open refocuses the field; Escape clears, then unfocuses.
 */
public final class UiSearchBox {

    private static final float WIDTH = 160.0F;
    private static final float HEIGHT = 18.0F;

    private final Runnable onChange;
    private final StringBuilder text = new StringBuilder();
    private boolean focused;
    private float focusAnim;

    public UiSearchBox(Runnable onChange) {
        this.onChange = onChange;
    }

    public String text() {
        return text.toString();
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            text.append(value);
        }
        changed();
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void update() {
        float target = focused ? 1.0F : 0.0F;
        focusAnim += (target - focusAnim) * Math.min(1.0F, 0.25F);
    }

    public void draw(float x, float y, double mx, double my) {
        boolean hot = mx >= x && mx < x + WIDTH && my >= y && my < y + HEIGHT;
        UiCanvas.roundRect(x, y, WIDTH, HEIGHT, 6.0F,
            focused || hot ? UiTheme.withAlpha(UiTheme.cardHover(), 250)
                : UiTheme.withAlpha(UiTheme.card(), 240));
        UiCanvas.outline(x, y, WIDTH, HEIGHT, 6.0F,
            focused ? UiTheme.withAlpha(UiTheme.accent(), 160) : UiTheme.edge(), 1.0F);

        float textY = y + (HEIGHT - AetherFont.height(9.0F)) / 2.0F;
        AetherFont.drawIcon(UiIcon.SEARCH, 9.0F, x + 5.0F, textY, UiTheme.textSoft());

        float slide = (1.0F - focusAnim) * 8.0F;
        if (text.length() == 0) {
            AetherFont.draw(9.0F, "Search", x + 16.0F + slide, textY,
                UiTheme.withAlpha(UiTheme.textFaint(), Math.round(200 * (1.0F - focusAnim * 0.7F))));
        } else {
            AetherFont.draw(9.0F, text.toString(), x + 16.0F, textY, UiTheme.text());
        }
        if (focused && (System.currentTimeMillis() / 600L) % 2L == 0L) {
            float caretX = x + 16.0F + AetherFont.width(9.0F, text.toString());
            UiCanvas.roundRect(caretX, textY, 1.0F, AetherFont.height(9.0F), 0.0F, UiTheme.text());
        }
    }

    public boolean click(float x, float y, double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        if (mx >= x && mx < x + WIDTH && my >= y && my < y + HEIGHT) {
            focused = true;
            return true;
        }
        return false;
    }

    /** Defocuses when a click lands elsewhere; Escape clears then unfocuses. */
    public boolean clickOutside(double mx, double my) {
        if (focused) {
            focused = false;
            return true;
        }
        return false;
    }

    /** @return true when the key was consumed (the field is focused). */
    public boolean key(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == 14) { // backspace
            if (text.length() > 0) {
                text.deleteCharAt(text.length() - 1);
                changed();
            }
            return true;
        }
        if (keyCode == 1) { // escape: clear, then unfocus on the second press
            if (text.length() > 0) {
                text.setLength(0);
                changed();
            } else {
                focused = false;
            }
            return true;
        }
        if (typedChar >= 32 && typedChar < 127 && text.length() < 40) {
            text.append(typedChar);
            changed();
            return true;
        }
        return keyCode != 0;
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
