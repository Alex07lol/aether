package dev.aether.menu.comp;

import java.util.List;

import dev.aether.gui.AetherFont;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * The choice control of the grammar: a 75x16 accent-gradient stepper with the current
 * option centred and click zones at both ends - never a popup list. Clicking an end
 * steps the option and plays the vanilla button sound.
 */
public final class UiCombo {

    private final List<String> options;
    private final IntReader reader;
    private final IntWriter writer;

    public interface IntReader {
        int read();
    }

    public interface IntWriter {
        void write(int index);
    }

    public UiCombo(List<String> options, IntReader reader, IntWriter writer) {
        this.options = options;
        this.reader = reader;
        this.writer = writer;
    }

    /** Draws the 75x16 stepper at (x, y); returns the hit zone layout for click handling. */
    public void draw(float x, float y, double mx, double my) {
        boolean hot = hits(x, y, mx, my);
        UiCanvas.gradientRoundRect(x, y, 75.0F, 16.0F, 4.0F,
            UiTheme.withAlpha(UiTheme.accent(), hot ? 255 : 230),
            UiTheme.withAlpha(UiTheme.accentDeep(), hot ? 255 : 230));
        String label = current();
        String shown = AetherFont.trim(8.0F, label, 55);
        AetherFont.drawCentered(8.0F, shown, x + 10.0F, y + (16.0F - AetherFont.height(8.0F)) / 2.0F,
            55.0F, UiTheme.readableOn(UiTheme.accent()));
        AetherFont.drawIcon(UiIcon.CHEVRON_LEFT, 10.0F, x + 4.0F,
            y + (16.0F - AetherFont.height(10.0F)) / 2.0F, UiTheme.readableOn(UiTheme.accent()));
        AetherFont.drawIcon(UiIcon.CHEVRON_RIGHT, 10.0F, x + 75.0F - 10.0F - AetherFont.iconWidth(UiIcon.CHEVRON_RIGHT, 10.0F),
            y + (16.0F - AetherFont.height(10.0F)) / 2.0F, UiTheme.readableOn(UiTheme.accent()));
    }

    private String current() {
        if (options == null || options.isEmpty()) {
            return "-";
        }
        int index = reader.read();
        if (index < 0 || index >= options.size()) {
            index = 0;
        }
        return options.get(index);
    }

    /** Click zones: the left 16px steps down, the right 16px steps up, like the reference. */
    public boolean click(float x, float y, double mx, double my, int button) {
        if (button != 0 || options == null || options.isEmpty() || !hits(x, y, mx, my)) {
            return false;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        int size = options.size();
        int index = reader.read();
        if (index < 0 || index >= size) {
            index = 0;
        }
        boolean left = mx < x + 16.0F;
        boolean right = mx >= x + 75.0F - 16.0F;
        if (left) {
            index = (index - 1 + size) % size;
        } else if (right) {
            index = (index + 1) % size;
        } else {
            index = (index + 1) % size;
        }
        writer.write(index);
        return true;
    }

    public boolean hits(float x, float y, double mx, double my) {
        return mx >= x && mx < x + 75.0F && my >= y && my < y + 16.0F;
    }
}
