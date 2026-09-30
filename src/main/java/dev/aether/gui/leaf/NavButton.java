package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.SystemButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a fixed-size clickable tile that plays the vanilla button
 * press sound and fires one action. Leaf draws it from a texture; Aether draws the
 * same shape procedurally in the Aether palette.
 * <p>
 * In the Leaf composition the four navigation tiles are 170x106 at
 * x = 430, 650, 1100, 1320, y = 250, and the "home" tile is 80x80 - the sizes this
 * component is used with.
 */
public class NavButton extends UiComponent {

    private final String label;
    private final Runnable action;
    /** Visual state flags mirroring Leaf's isCover logic. */
    private boolean hover;
    /** The active navigation tile keeps a distinct tint (Leaf tinted it per state). */
    private boolean active;

    public NavButton(String label, int x, int y, int width, int height, Runnable action) {
        this.label = label;
        this.action = action;
        at(x, y).size(width, height);
    }

    public NavButton setActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        int radius = GuiScale.h(10);
        int fill = active ? AetherUi.withAlpha(AetherUi.ACCENT, 0x40)
            : hover ? AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xEE) : AetherUi.withAlpha(AetherUi.GLASS, 0xCC);
        AetherUi.drawRoundRect(left, top, left + w, top + h, radius, fill);
        AetherUi.outline(left, top, left + w, top + h,
            active || hover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x88) : AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x40));
        int labelSize = AetherFont.height(AetherFont.Size.SECTION);
        String shown = AetherFont.trimTo(AetherFont.Size.SECTION, label, w - GuiScale.w(10));
        AetherFont.drawCenteredShadowed(AetherFont.Size.SECTION, shown, left,
            top + (h - labelSize) / 2, w, active ? AetherUi.ACCENT : hover ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
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
        if (action != null) {
            action.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }
}
