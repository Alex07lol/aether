package dev.aether.gui.leaf;

import java.util.List;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.SelectButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a pill showing "{@code < current >}" that cycles through its
 * options on click, with the row's name drawn to the left of the pill (Leaf draws it
 * 210 design units left of the pill's edge). Used for the cosmetics category selector
 * and for choice settings.
 */
public final class SelectButton extends UiComponent {

    private final String rowName;
    private final List<String> options;
    private int index;
    private final Runnable onChange;
    private boolean hover;

    public SelectButton(String rowName, int x, int y, int width, int height, List<String> options,
                        String current, Runnable onChange) {
        this.rowName = rowName;
        this.options = options;
        this.index = Math.max(0, options.indexOf(current));
        this.onChange = onChange;
        at(x, y).size(width, height);
    }

    public String current() {
        if (options.isEmpty()) {
            return "-";
        }
        if (index < 0 || index >= options.size()) {
            index = 0;
        }
        return options.get(index);
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

        int fill = hover ? AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xEE) : AetherUi.withAlpha(AetherUi.GLASS, 0xCC);
        AetherUi.drawRoundRect(left, top, left + w, top + h, GuiScale.h(10), fill);
        AetherUi.outline(left, top, left + w, top + h,
            hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x66) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));

        int labelSize = AetherFont.height(AetherFont.Size.BODY);
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, "< " + current() + " >", w - GuiScale.w(16));
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left, top + (h - labelSize) / 2, w, AetherUi.TEXT_PRIMARY);
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY) || options.isEmpty()) {
            return false;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        index = (index + 1) % options.size();
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
