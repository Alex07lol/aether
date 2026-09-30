package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.CosmeticButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a 300x90 selectable pill with the entry name centered and a click callback.
 * Leaf drew it from the same {@code select.png} as its select buttons and told the states apart by
 * tint - white for the equipped entry, its dark tone for the rest - which here is the brightness of
 * the art. The entries sit at x = 480 starting at y = 400 with a pitch of 100.
 */
public final class CosmeticEntry extends UiComponent {

    private final String name;
    private final boolean selected;
    private final Runnable onSelect;
    private boolean hover;

    public CosmeticEntry(String name, int x, int y, int width, int height, boolean selected, Runnable onSelect) {
        this.name = name;
        this.selected = selected;
        this.onSelect = onSelect;
        at(x, y).size(width, height);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        float brightness = selected ? LeafArt.BRIGHT : LeafArt.NORMAL;

        if (hover) {
            LeafArt.drawHovered(LeafArt.SELECT, left, top, w, h, brightness);
        } else {
            LeafArt.draw(LeafArt.SELECT, left, top, w, h, brightness);
        }

        int labelSize = AetherFont.height(AetherFont.Size.BODY);
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, name, w - GuiScale.w(20));
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left, top + (h - labelSize) / 2, w,
            selected ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
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
        if (onSelect != null) {
            onSelect.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
