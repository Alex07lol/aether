package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.ToggleButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a switch tile whose row label is drawn far to the left of the control -
 * Leaf draws the label 410 design units left of the tile's edge, which is what lines the labels up
 * in the ClientSettings composition. Leaf swapped between {@code true.png} and {@code false.png}
 * for the state; the art carries the state in the knob's position, and the brightness keeps the
 * distinction readable in black and white. Clicking flips the state and fires the callback.
 */
public final class LeafToggle extends UiComponent {

    private final String rowName;
    private final StateReader reader;
    private final Runnable onChange;
    private boolean hover;

    public interface StateReader {
        boolean isOn();
    }

    public LeafToggle(String rowName, int x, int y, int width, int height, StateReader reader, Runnable onChange) {
        this.rowName = rowName;
        this.reader = reader;
        this.onChange = onChange;
        at(x, y).size(width, height);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        boolean on = reader.isOn();

        if (rowName != null && !rowName.isEmpty()) {
            int labelSize = AetherFont.height(AetherFont.Size.BODY);
            AetherFont.draw(AetherFont.Size.BODY, rowName, left - GuiScale.w(410),
                top + (h - labelSize) / 2, on ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
        }

        String art = on ? LeafArt.TOGGLE_ON : LeafArt.TOGGLE_OFF;
        float brightness = on ? LeafArt.BRIGHT : LeafArt.NORMAL;
        if (hover) {
            LeafArt.drawHovered(art, left, top, w, h, brightness);
        } else {
            LeafArt.draw(art, left, top, w, h, brightness);
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
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        if (onChange != null) {
            onChange.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
