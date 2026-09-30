package dev.aether.gui.leaf;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.TextBox} (GPLv3, see
 * docs/GUI_REBUILD.md): a single-line field that commits on deselect (Leaf calls
 * {@code doThings()} when a click lands outside), with a caret while focused,
 * backspace, and Ctrl+V clipboard paste. Leaf draws a square icon at the field's right
 * edge; Aether draws a clear (x) glyph in that same square instead.
 */
public final class LeafTextBox extends UiComponent {

    private final String rowName;
    private final Runnable onCommit;
    private String text = "";
    private boolean focused;
    private boolean hover;
    private boolean pasteLatch;

    public LeafTextBox(String rowName, int x, int y, int width, int height, String initial, Runnable onCommit) {
        this.rowName = rowName;
        this.onCommit = onCommit;
        if (initial != null) {
            this.text = initial;
        }
        at(x, y).size(width, height);
    }

    public String text() {
        return text;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();

        if (rowName != null && !rowName.isEmpty()) {
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(410),
                top + (h - labelSize) / 2, AetherUi.TEXT_SECONDARY);
        }

        int fill = focused ? AetherUi.withAlpha(AetherUi.SEARCH_FOCUS, 0xF0)
            : hover ? AetherUi.withAlpha(AetherUi.SEARCH, 0xF0) : AetherUi.withAlpha(AetherUi.SEARCH, 0xD9);
        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(8), fill);
        AetherUi.outline(left, top, left + w, top + h,
            focused ? AetherUi.withAlpha(AetherUi.ACCENT, 0x88) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));

        int pad = GuiScale.w(14);
        int textSize = AetherFont.height(AetherFont.Size.BODY);
        int textY = top + (h - textSize) / 2;

        String shown = AetherFont.trimTo(AetherFont.Size.BODY, text, w - pad * 2 - GuiScale.w(24));
        AetherFont.draw(AetherFont.Size.BODY, shown, left + pad, textY,
            text.isEmpty() && !focused ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY);
        if (focused) {
            boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
            if (blink) {
                int caretX = left + pad + AetherFont.width(AetherFont.Size.BODY, shown) + GuiScale.w(1);
                Mc189Compat.drawRect(caretX, textY, caretX + GuiScale.w(1), textY + textSize, AetherUi.ACCENT);
            }
        }

        // Clear glyph in the right-edge square where Leaf drew its show/hide icon.
        int s = GuiScale.h(10);
        int cx = left + w - pad - s;
        int cy = top + (h - s) / 2;
        int color = text.isEmpty() ? AetherUi.withAlpha(AetherUi.TEXT_DISABLED, 0x66)
            : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY;
        for (int i = 0; i <= s; i++) {
            Mc189Compat.drawRect(cx + i - s / 2, cy + i - s / 2, cx + i - s / 2 + 1, cy + i - s / 2 + 1, color);
            Mc189Compat.drawRect(cx + s - i - s / 2, cy + i - s / 2, cx + s - i - s / 2 + 1, cy + i - s / 2 + 1, color);
        }
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
        if (!text.isEmpty()) {
            int pad = 14;
            int s = 10;
            double clearX = x + width - pad - s;
            if (mouseX >= clearX - s && mouseX <= clearX + s * 2.0D) {
                text = "";
                commit();
                return true;
            }
        }
        focused = true;
        return true;
    }

    /** Leaf semantics: a click anywhere else commits the edit and drops focus. */
    public void clickElsewhere() {
        if (focused) {
            focused = false;
            commit();
        }
    }

    private void commit() {
        if (onCommit != null) {
            onCommit.run();
        }
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == 14) { // backspace
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
            return true;
        }
        if (keyCode == 29) { // left ctrl arms paste, like Leaf
            pasteLatch = true;
            return true;
        }
        if (keyCode == 47 && pasteLatch) { // V pastes the clipboard
            pasteLatch = false;
            try {
                Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (data instanceof String) {
                    text = text + (String) data;
                }
            } catch (Exception ignored) {
            }
            return true;
        }
        if (keyCode == 28 || keyCode == 1) { // return / esc commit and close
            focused = false;
            commit();
            return true;
        }
        if (keyCode == 42 || keyCode == 54 || keyCode == 57 || keyCode == 200 || keyCode == 203 || keyCode == 205 || keyCode == 208) {
            pasteLatch = false;
            return true;
        }
        pasteLatch = false;
        if (typedChar >= 32 && typedChar < 127 && text.length() < 64) {
            text = text + typedChar;
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
