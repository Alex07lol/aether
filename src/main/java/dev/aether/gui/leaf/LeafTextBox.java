package dev.aether.gui.leaf;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.TextBox} (GPLv3, see docs/GUI_REBUILD.md):
 * Leaf's {@code field/search.png} stretched over the row, a single-line value that commits on
 * deselect (Leaf calls {@code doThings()} when a click lands outside), with a caret while focused,
 * backspace, and Ctrl+V clipboard paste. An optional row label is drawn 210 design units to the
 * left, the same offset Leaf's select buttons use, which is what lines a text row up with the
 * choice rows beside it.
 * <p>
 * The one addition is a clear button in the square at the field's right edge where Leaf draws its
 * icon: clearing through a visible control beats an invisible click zone, and it is drawn from the
 * same art set (Leaf's {@code close.png}).
 */
public final class LeafTextBox extends UiComponent {

    /** The clear button's square, in design units, inset from the field's right edge. */
    private static final int CLEAR_SIZE = 34;
    private static final int CLEAR_INSET = 8;

    private final String rowName;
    private final Runnable onCommit;
    private String text = "";
    private String placeholder = "";
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

    /** Text shown greyed out while the field is empty; the field has no other caption. */
    public LeafTextBox setPlaceholder(String value) {
        this.placeholder = value == null ? "" : value;
        return this;
    }

    public String text() {
        return text;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
    }

    private int clearLeft() {
        return (int) Math.round(x + width - CLEAR_INSET - CLEAR_SIZE);
    }

    private boolean overClear(double mouseX, double mouseY) {
        int left = clearLeft();
        double top = y + (height - CLEAR_SIZE) / 2.0D;
        return mouseX >= left && mouseX <= left + CLEAR_SIZE && mouseY >= top && mouseY <= top + CLEAR_SIZE;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();

        if (rowName != null && !rowName.isEmpty()) {
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(210),
                top + (h - labelSize) / 2, AetherUi.TEXT_SECONDARY);
        }

        LeafArt.draw(LeafArt.FIELD, left, top, w, h, focused ? LeafArt.BRIGHT : LeafArt.NORMAL);

        int pad = GuiScale.w(16);
        int textSize = AetherFont.height(AetherFont.Size.BODY);
        int textY = top + (h - textSize) / 2;
        int textSpace = w - pad - GuiScale.w(CLEAR_INSET + CLEAR_SIZE + 8);

        boolean empty = text.isEmpty();
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, empty ? placeholder : text, textSpace);
        AetherFont.draw(AetherFont.Size.BODY, shown, left + pad, textY,
            empty ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_PRIMARY);
        if (focused) {
            boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
            if (blink) {
                int caretX = left + pad + AetherFont.width(AetherFont.Size.BODY, shown) + GuiScale.w(1);
                Mc189Compat.drawRect(caretX, textY, caretX + GuiScale.w(1), textY + textSize, AetherUi.ACCENT);
            }
        }

        if (!text.isEmpty()) {
            int clear = GuiScale.w(CLEAR_SIZE);
            LeafArt.draw(LeafArt.CLOSE, left + w - GuiScale.w(CLEAR_INSET) - clear,
                top + (h - clear) / 2, clear, clear, LeafArt.BRIGHT);
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
        if (!text.isEmpty() && overClear(mouseX, mouseY)) {
            text = "";
            commit();
            return true;
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
