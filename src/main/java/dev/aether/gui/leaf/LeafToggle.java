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
 * docs/GUI_REBUILD.md): a state tile whose row label is drawn far to the left of the
 * control - Leaf draws the label 410 design units left of the tile's left edge, which
 * is what lines the labels up in the ClientSettings composition. Clicking flips the
 * state and fires the callback.
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

        int fill = on ? AetherUi.withAlpha(AetherUi.ACCENT_ON, hover ? 0xFF : 0xD9)
            : hover ? AetherUi.withAlpha(AetherUi.TOGGLE_BG, 0xFF) : AetherUi.withAlpha(AetherUi.TOGGLE_BG, 0xCC);
        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(8), fill);
        AetherUi.outline(left, top, left + w, top + h,
            on ? AetherUi.withAlpha(AetherUi.ACCENT_ON, 0x66) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));
        int labelSize = AetherFont.height(AetherFont.Size.SMALL);
        AetherFont.drawCenteredShadowed(AetherFont.Size.SMALL, on ? "ON" : "OFF", left,
            top + (h - labelSize) / 2, w, on ? 0xFF0B1210 : AetherUi.TEXT_SECONDARY);
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
