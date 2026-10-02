package dev.aether.menu.comp;

import org.lwjgl.input.Keyboard;

import dev.aether.gui.AetherFont;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiTheme;

/**
 * The keybind control of the grammar: a 75x16 accent stepper-shaped pill showing the
 * bound key's name, or "Binding..." while a capture is live. Clicking starts the
 * capture through a callback the screen wires into {@code client.input()}; Escape
 * cancels, and any other key binds.
 */
public final class UiKeybindBox {

    private final KeyReader reader;
    private final Runnable onStartCapture;

    public interface KeyReader {
        int keyCode();
    }

    public UiKeybindBox(KeyReader reader, Runnable onStartCapture) {
        this.reader = reader;
        this.onStartCapture = onStartCapture;
    }

    public void draw(float x, float y, boolean capturing, double mx, double my) {
        boolean hot = hits(x, y, mx, my);
        UiCanvas.gradientRoundRect(x, y, 75.0F, 16.0F, 4.0F,
            capturing ? UiTheme.accent() : UiTheme.withAlpha(UiTheme.accent(), hot ? 255 : 230),
            capturing ? UiTheme.accentDeep() : UiTheme.withAlpha(UiTheme.accentDeep(), hot ? 255 : 230));
        String label = capturing ? "Binding..." : Keyboard.getKeyName(reader.keyCode());
        AetherFont.drawCentered(8.0F, AetherFont.trim(8.0F, label, 65), x,
            y + (16.0F - AetherFont.height(8.0F)) / 2.0F, 75.0F, UiTheme.readableOn(UiTheme.accent()));
    }

    public boolean click(float x, float y, double mx, double my, int button) {
        if (button != 0 || !hits(x, y, mx, my)) {
            return false;
        }
        if (onStartCapture != null) {
            onStartCapture.run();
        }
        return true;
    }

    public boolean hits(float x, float y, double mx, double my) {
        return mx >= x && mx < x + 75.0F && my >= y && my < y + 16.0F;
    }
}
