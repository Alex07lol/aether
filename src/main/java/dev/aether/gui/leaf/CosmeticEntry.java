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
 * docs/GUI_REBUILD.md): a 300x90 selectable pill with the entry name centered, a
 * distinct tint while it is the selected cosmetic, and a click callback. In the Leaf
 * composition the entries sit at x = 480 starting at y = 400 with a pitch of 100.
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
        int fill = selected ? AetherUi.withAlpha(AetherUi.ACCENT, 0x40)
            : hover ? AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xEE) : AetherUi.withAlpha(AetherUi.GLASS, 0xB3);
        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(10), fill);
        AetherUi.outline(left, top, left + w, top + h,
            selected ? AetherUi.withAlpha(AetherUi.ACCENT, 0x99) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));

        int labelSize = AetherFont.height(AetherFont.Size.BODY);
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, name, w - GuiScale.w(20));
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left,
            top + (h - labelSize) / 2, w, selected ? AetherUi.ACCENT : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
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
