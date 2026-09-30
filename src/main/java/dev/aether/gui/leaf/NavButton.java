package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.SystemButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a fixed-size tile drawn from one stretched texture that plays the vanilla
 * button press sound and fires one action. Leaf's tiles carry their own wording in the art, so a
 * tile passes {@code null} for {@link #label} and only tiles that need a caption Aether's art does
 * not have - the settings actions, for instance - draw text on top.
 * <p>
 * In the Leaf composition the four navigation tiles are 170x106 at x = 430, 650, 1100, 1320, y =
 * 250, and the "home" tile is 80x80.
 */
public class NavButton extends UiComponent {

    /** The section this tile belongs to is the one on screen. */
    private static final float ACTIVE = LeafArt.BRIGHT;
    /** A tile that is available but not the current screen; Leaf had no such state, so the tile
     * keeps its own art and only dims. */
    private static final float IDLE = 0.70F;

    private final String art;
    private final String label;
    private final Runnable action;
    private boolean hover;
    private boolean active = true;

    /** A tile whose wording is baked into its art. */
    public NavButton(String art, int x, int y, int width, int height, Runnable action) {
        this(art, null, x, y, width, height, action);
    }

    /** A tile that draws {@code label} over its art. */
    public NavButton(String art, String label, int x, int y, int width, int height, Runnable action) {
        this.art = art;
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
        float brightness = active ? ACTIVE : IDLE;

        if (hover) {
            LeafArt.drawHovered(art, left, top, w, h, brightness);
        } else {
            LeafArt.draw(art, left, top, w, h, brightness);
        }

        if (label != null) {
            int size = AetherFont.height(AetherFont.Size.BODY);
            String shown = AetherFont.trimTo(AetherFont.Size.BODY, label, w - GuiScale.w(12));
            AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left, top + (h - size) / 2, w,
                active ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);
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
