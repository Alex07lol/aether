package dev.aether.gui.components;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

import java.util.function.Consumer;

/**
 * The single-line text field of the Aether GUI, the equivalent of Leaf Client's
 * {@code TextBox} concept (see docs/GUI_REBUILD.md), rebuilt with Aether's font and
 * the interaction rules the brief demands: focus state, blinking caret, backspace,
 * a clear button, live change notifications and a placeholder that disappears the
 * moment the field has content.
 */
public final class TextField extends UiComponent {

    private static final int KEY_BACKSPACE = 14;
    private static final int KEY_RETURN = 28;
    private static final int MAX_LENGTH = 64;

    private final String placeholder;
    private final Consumer<String> onChange;
    private String text = "";
    private boolean focused;
    private boolean hover;

    public TextField(String placeholder, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.onChange = onChange;
        this.height = 34;
    }

    public String text() {
        return text;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    /** Clears the field without notifying (used when a caller resets it wholesale). */
    public void reset() {
        text = "";
        focused = false;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        int radius = GuiScale.h(8);
        AetherUi.drawRoundRect(left, top, left + w, top + h, radius,
            focused ? AetherUi.SEARCH_FOCUS : hover ? AetherUi.SEARCH : AetherUi.withAlpha(AetherUi.SEARCH, 0xD9));
        AetherUi.outline(left, top, left + w, top + h,
            focused ? AetherUi.withAlpha(AetherUi.ACCENT, 0x88) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));

        boolean showPlaceholder = text.isEmpty();
        String visible = showPlaceholder ? placeholder : text;
        int fontSize = AetherFont.height(AetherFont.Size.BODY);
        int textY = top + (h - fontSize) / 2;
        int color = showPlaceholder ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY;

        // Reserve the tail of the field for the clear button when it is shown.
        int textRight = left + w - GuiScale.w(12);
        if (!text.isEmpty()) {
            textRight -= GuiScale.w(18);
        }
        int maxTextW = textRight - (left + GuiScale.w(12));
        String fitted = AetherFont.trimTo(AetherFont.Size.BODY, visible, Math.max(0, maxTextW));
        AetherFont.draw(AetherFont.Size.BODY, fitted, left + GuiScale.w(12), textY, color);

        if (focused) {
            boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
            if (blink) {
                int caretX = left + GuiScale.w(12) + AetherFont.width(AetherFont.Size.BODY, fitted) + GuiScale.w(1);
                Mc189Compat.drawRect(caretX, textY, caretX + GuiScale.w(1), textY + fontSize, AetherUi.ACCENT);
            }
        }

        if (!text.isEmpty()) {
            drawClearButton(left + w - GuiScale.w(22), top + (h - GuiScale.h(12)) / 2);
        }
    }

    private void drawClearButton(int cx, int cy) {
        int color = hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY;
        int s = GuiScale.h(10);
        int g = Math.max(1, GuiScale.h(2));
        for (int i = 0; i <= s - g; i++) {
            Mc189Compat.drawRect(cx + i, cy + i, cx + i + g, cy + i + g, color);
            Mc189Compat.drawRect(cx + s - i - g, cy + i, cx + s - i, cy + i + g, color);
        }
    }

    private boolean inClearButton(double mouseX, double mouseY) {
        if (text.isEmpty()) {
            return false;
        }
        double left = x + width - 26;
        double top = y + (height - 18) / 2.0D;
        return mouseX >= left && mouseX <= left + 22 && mouseY >= top && mouseY <= top + 18;
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
        if (inClearButton(mouseX, mouseY)) {
            text = "";
            notifyChanged();
            return true;
        }
        focused = true;
        return true;
    }

    /** Defocuses when the click lands elsewhere; the screen calls this on every click. */
    public void clickOutside() {
        focused = false;
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == KEY_BACKSPACE) {
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                notifyChanged();
            }
            return true;
        }
        if (keyCode == KEY_RETURN) {
            focused = false;
            return true;
        }
        if (typedChar >= 32 && typedChar < 127 && text.length() < MAX_LENGTH) {
            text = text + typedChar;
            notifyChanged();
            return true;
        }
        return keyCode != 0;
    }

    private void notifyChanged() {
        if (onChange != null) {
            onChange.accept(text);
        }
    }
}
